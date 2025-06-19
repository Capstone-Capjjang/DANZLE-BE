import cv2
import mediapipe as mp
import math
import numpy as np
import threading
import logging
logging.basicConfig(level=logging.DEBUG)

import sys
sys.stdout.reconfigure(line_buffering=True)

pose_detector_lock = threading.Lock()
_detector = None

def get_pose_detector():
    global _detector
    if _detector is None:
        mp_pose = mp.solutions.pose
        _detector = mp_pose.Pose(static_image_mode=False)
    return _detector

def close_pose_detector():
    global _detector
    if _detector:
        _detector.close()
        _detector = None


# 관절 각도 계산
def compute_angle(a, b, c):
    a = np.array([a['x'], a['y']])
    b = np.array([b['x'], b['y']])
    c = np.array([c['x'], c['y']])

    ba = a - b
    bc = c - b

    cosine_angle = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc))
    return np.degrees(np.arccos(np.clip(cosine_angle, -1.0, 1.0)))

# 정규화 함수 (안무가/사용자 공통 적용 가능)
def normalize_pose_keypoints(extracted):
    base_x = (extracted["left_hip"].x + extracted["right_hip"].x) / 2
    base_y = (extracted["left_hip"].y + extracted["right_hip"].y) / 2
    norm_sq = sum((v.x - base_x) ** 2 + (v.y - base_y) ** 2 + v.z ** 2 for v in extracted.values())
    l2 = math.sqrt(norm_sq) or 1.0
    normalized = {
        name: {"x": (v.x - base_x) / l2, "y": (v.y - base_y) / l2, "z": v.z / l2}
        for name, v in extracted.items()
    }
    normalized["mid_hip"] = {"x": 0.0, "y": 0.0, "z": 0.0}
    return normalized

# 사용자 포즈 추출 (정규화 포함)
def extract_pose_keypoints(frame):
    detector = get_pose_detector()
    image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    with pose_detector_lock:
        results = detector.process(image_rgb)

    if not results.pose_landmarks:
        return None

    landmarks = results.pose_landmarks.landmark
    indices = {
        "left_shoulder": 11, "right_shoulder": 12, "left_elbow": 13, "right_elbow": 14,
        "left_wrist": 15, "right_wrist": 16, "left_hip": 23, "right_hip": 24,
        "left_knee": 25, "right_knee": 26, "left_ankle": 27, "right_ankle": 28
    }
    extracted = {name: landmarks[idx] for name, idx in indices.items()}
    return normalize_pose_keypoints(extracted)

# 부위별 거리 오차
def part_diff(user, ref, keys):
    return (sum(abs(user[k]['x'] - ref[k]['x']) +
                abs(user[k]['y'] - ref[k]['y']) +
                abs(user[k]['z'] - ref[k]['z']) for k in keys) / len(keys)) * 100

# 방향 및 포즈 정확도 비교
def compare_pose_directional(user, ref):
    if user is None or ref is None:
        return 0.0

    def angle(pose, joints):
        try:
            return compute_angle(pose[joints[0]], pose[joints[1]], pose[joints[2]])
        except:
            return 0.0

    # 허용 오차 및 감점 완화
    clip_threshold = 4.0
    tolerance_scale = 2.0

    def process_diff(diff):
        return max(0, (diff - clip_threshold) / tolerance_scale)

    # 방향 점수
    dx_user = user['right_shoulder']['x'] - user['left_shoulder']['x']
    dy_user = user['right_shoulder']['y'] - user['left_shoulder']['y']
    user_angle = math.degrees(math.atan2(dy_user, dx_user))

    dx_ref = ref['right_shoulder']['x'] - ref['left_shoulder']['x']
    dy_ref = ref['right_shoulder']['y'] - ref['left_shoulder']['y']
    ref_angle = math.degrees(math.atan2(dy_ref, dx_ref))

    diff_angle = ((user_angle - ref_angle + 180) % 360) - 180
    direction_score = max(0, 100 - process_diff(abs(diff_angle)))

    body_parts = {
        'left_arm': ["left_shoulder", "left_elbow", "left_wrist"],
        'right_arm': ["right_shoulder", "right_elbow", "right_wrist"],
        'left_leg': ["left_hip", "left_knee", "left_ankle"],
        'right_leg': ["right_hip", "right_knee", "right_ankle"]
    }

    breakdown = {
        part: max(0, 100 - process_diff(part_diff(user, ref, joints)))
        for part, joints in body_parts.items()
    }

    angle_scores = {
        part: max(0, 100 - process_diff(abs(angle(user, joints) - angle(ref, joints))))
        for part, joints in body_parts.items()
    }

    weights = {
        'direction': 2.0,
        'left_arm': 1.0,
        'right_arm': 1.0,
        'left_leg': 1.0,
        'right_leg': 1.0,
        'angle_left_arm': 1.0,
        'angle_right_arm': 1.0,
        'angle_left_leg': 1.0,
        'angle_right_leg': 1.0
    }

    total_score = (
        direction_score * weights['direction'] +
        breakdown['left_arm'] * weights['left_arm'] +
        breakdown['right_arm'] * weights['right_arm'] +
        breakdown['left_leg'] * weights['left_leg'] +
        breakdown['right_leg'] * weights['right_leg'] +
        angle_scores['left_arm'] * weights['angle_left_arm'] +
        angle_scores['right_arm'] * weights['angle_right_arm'] +
        angle_scores['left_leg'] * weights['angle_left_leg'] +
        angle_scores['right_leg'] * weights['angle_right_leg']
    ) / sum(weights.values())

    # 디버깅 출력
    logging.debug(f"direction: {direction_score:.2f}")
    for part in body_parts:
        logging.debug(f"{part}: dist={breakdown[part]:.2f}, angle={angle_scores[part]:.2f}")

    return round(total_score, 2)

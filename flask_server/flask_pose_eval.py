from flask import Flask, request, jsonify
import cv2
import gc
import numpy as np
import json
from collections import deque
from pose import extract_pose_keypoints, compare_pose_bdp, compare_pose_bdp_double_frame, close_pose_detector
import logging
logging.basicConfig(level=logging.DEBUG)


app = Flask(__name__)

# 세션별로 2개 프레임까지 저장하는 큐
user_pose_queue = {}
ref_pose_queue = {}


################# S3에서 안무가 JSON 불러오기 (캐시식 구조로 반복 요청 대비) ##################
pose_cache = {}

def get_ref_pose_from_disk(song_title: str, frame_index: int):
    if song_title not in pose_cache:
        file_path = f"./ref_poses/{song_title}_ref_pose_filtered_1sec_normalized.json"
        try:
            with open(file_path, "r") as f:
                pose_dict_raw = json.load(f)
                pose_dict = {int(item["frame"]): item["keypoints"] for item in pose_dict_raw}
                pose_cache[song_title] = pose_dict
        except FileNotFoundError:
            app.logger.error(f"Reference pose file not found for {song_title}")
            return None
        except json.JSONDecodeError:
            app.logger.error(f"Error decoding JSON from {file_path}")
            return None

    return pose_cache[song_title].get(frame_index)


################# 실시간 포즈 평가 API ##################
@app.route("/analyze", methods=["POST"])
def pose_eval():
    image = request.files.get("frame")
    song_title = request.form.get("song_title")
    session_id = request.form.get("session_id")
    frame_index = int(request.form.get("frame_index", 0))

    app.logger.info(f"image={image}, song_title={song_title}, session_id={session_id}, frame_index={frame_index}")

    if not image or not song_title:
        app.logger.warning("Missing parameters")
        return jsonify({"error": "Missing parameters"}), 400

    npimg = np.frombuffer(image.read(), np.uint8)
    frame = cv2.imdecode(npimg, cv2.IMREAD_COLOR)

    del npimg  # 불필요한 numpy 배열 제거


    # 사용자 프레임 → 키포인트 추출
    user_kps = extract_pose_keypoints(frame)
    if user_kps is None:
        app.logger.warning("User pose not detected in the frame")
        return jsonify({
        "score": 0,
        "feedback": "WORST",
        "frame_index": frame_index
        })


    # 전문가 키포인트 불러오기
    expert_kps = get_ref_pose_from_disk(song_title, frame_index)

    if expert_kps is None:
        app.logger.warning(f"No reference keypoints for frame {frame_index} of {song_title}")
        return jsonify({"error": f"No reference pose for frame {frame_index}"}), 400


    # 세션별 큐 초기화 (처음 요청일 경우)
    if session_id not in user_pose_queue:
        user_pose_queue[session_id] = deque(maxlen=2)
        ref_pose_queue[session_id] = deque(maxlen=2)

    # 큐에 현재 프레임 포즈 추가
    user_pose_queue[session_id].append(user_kps)
    ref_pose_queue[session_id].append(expert_kps)



    # 사용자와 전문가 키포인트 비교
    try:
        if len(user_pose_queue[session_id]) == 2 and len(ref_pose_queue[session_id]) == 2:
            user_prev, user_now = user_pose_queue[session_id]
            ref_prev, ref_now = ref_pose_queue[session_id]
            score = compare_pose_bdp_double_frame(user_now, user_prev, ref_now, ref_prev)
        else:
            score = compare_pose_bdp(user_kps, expert_kps)
    except Exception as e:
        app.logger.error(f"Pose comparison failed: {e}")
        return jsonify({"error": "Pose comparison failed"}), 500

    del frame
    del user_kps


    # 점수 산출
    if score >= 90:
        feedback = "Perfect"
    elif score >= 80:
        feedback = "Good"
    elif score >= 75:
        feedback = "Normal"
    elif score >= 60:
        feedback = "Bad"
    else:
        feedback = "Worst"

    return jsonify({
        "score": score,
        "feedback": feedback,
        "frame_index": frame_index
    })


################# 후처리 ##################

@app.route("/save", methods=["POST"])
def cleanMemory():
    close_pose_detector()
    user_pose_queue.clear()
    ref_pose_queue.clear()
    collected = gc.collect()
    app.logger.info(f"Detector closed, gc.collect() called. Objects collected: {collected}")
    return jsonify({"status": "closed", "objects_collected": collected})


@app.route("/health", methods=["GET"])
def healthcheck():
    return jsonify({"status": "UP"}), 200


if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000)

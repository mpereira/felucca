using System;
using UnityEngine;

namespace Felucca.Components {
    public class FollowingCamera : MonoBehaviour {
        public Camera mainCamera;

        public Creature followee;
        public Vector3  position;
        public Vector3  rotation;
        public bool     isFollowing;

        private void Start() {
            position = new Vector3(0, 43, 0);
            rotation = new Vector3(45, -45, 0);

            mainCamera = gameObject.GetComponent<Camera>();
            mainCamera.orthographic = true;
            mainCamera.orthographicSize = 10f;
            mainCamera.backgroundColor = Color.clear;
            mainCamera.clearFlags = CameraClearFlags.SolidColor;

            mainCamera.transform.localPosition = position;
            mainCamera.transform.localEulerAngles = rotation;
        }

        private void LateUpdate() {
            if (followee != null && isFollowing) {
                // FIXME: where are these magic numbers coming from? They are
                // likely related to the camera position y axis.
                position.x = followee.transform.position.x + 30;
                position.z = followee.transform.position.z - 30;
            }

            mainCamera.transform.localPosition = position;
            mainCamera.transform.localEulerAngles = rotation;
        }

        public void Follow(Creature creature) {
            followee = creature;
        }
    }
}

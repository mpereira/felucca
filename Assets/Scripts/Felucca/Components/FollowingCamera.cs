using System;
using UnityEngine;

namespace Felucca.Components {
    public class FollowingCamera : MonoBehaviour {
        public GameObject followee;
        public Vector3 position;
        public Vector3 rotation;
        public bool isFollowing;
        
        private Camera _camera;
        
        private void Start() {
            position = new Vector3(0, 43, 0);
            rotation = new Vector3(45, -45, 0);
            isFollowing = true;
            
            _camera = gameObject.GetComponent<Camera>();
            _camera.orthographic = true;
            _camera.orthographicSize = 10f;
            _camera.backgroundColor = Color.clear;
            _camera.clearFlags = CameraClearFlags.SolidColor;
            
            _camera.transform.localPosition = position;
            _camera.transform.localEulerAngles = rotation;
        }
        
        private void LateUpdate() {
            if (isFollowing) {
                // FIXME: where are these magic numbers coming from? They are
                // likely related to the camera position y axis.
                position.x = followee.transform.position.x + 30;
                position.z = followee.transform.position.z - 30;
            }
            
            _camera.transform.localPosition = position;
            _camera.transform.localEulerAngles = rotation;
        }
    }
}
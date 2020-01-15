using System;
using UnityEngine;

namespace Felucca.Components {
    public class FollowingCamera : MonoBehaviour {
        public GameObject followee;
        
        private Camera _camera;
        private Vector3 _position;
        
        private void Start() {
            _position = new Vector3(0, 50, 0);
            
            _camera = gameObject.GetComponent<Camera>();
            _camera.orthographic = true;
            _camera.orthographicSize = 10f;
            _camera.backgroundColor = Color.clear;
            _camera.transform.localPosition = _position;
            _camera.transform.localEulerAngles = new Vector3(30, -45, 0);
            _camera.clearFlags = CameraClearFlags.SolidColor;
        }
        
        private void LateUpdate() {
            // FIXME: where is this 60 coming from? It is likely related to the 
            // camera position y axis of 50.
            _position.x = followee.transform.position.x + 60;
            _position.z = followee.transform.position.z - 60;
            _camera.transform.localPosition = _position;
        }
    }
}
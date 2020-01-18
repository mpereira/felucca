using UnityEngine;

namespace Felucca {
    public static class DoubleClickHandler {
        private static int   _clicks           = 0;
        private static float _lastClickedAt    = 0f;
        private const  float MaximumClickDelay = 0.5f;

        public static bool DidDoubleClick() {
            if (Input.GetMouseButtonDown(0)) {
                _clicks++;
                if (_clicks == 1) {
                    _lastClickedAt = Time.time;
                }
            }

            if (_clicks > 1 && Time.time - _lastClickedAt < MaximumClickDelay) {
                _clicks = 0;
                _lastClickedAt = 0;
                return true;
            }

            if (_clicks > 2 || Time.time - _lastClickedAt > 1) {
                _clicks = 0;
            }

            return false;
        }
    }
}

using System.Linq;
using UnityEngine;

namespace Felucca {
    public static class TargetHitFinder {
        private static readonly RaycastHit[] HitResults = new RaycastHit[10];
        private static readonly Camera _camera;

        static TargetHitFinder() {
            _camera = Camera.main;
        }

        public static RaycastHit? TargetHit() {
            var ray = _camera.ScreenPointToRay(Input.mousePosition);

            Physics.RaycastNonAlloc(
                ray,
                HitResults,
                Mathf.Infinity,
                Physics.DefaultRaycastLayers
            );

            return HitResults.First();
        }
    }
}
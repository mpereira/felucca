using System;
using System.Linq;
using UnityEngine;
using UnityEngine.EventSystems;

namespace Felucca {
    public static class TargetHitFinder {
        private static readonly RaycastHit[] HitResults = new RaycastHit[10];
        private static readonly Camera       MainCamera;

        static TargetHitFinder() {
            MainCamera = Camera.main;
        }

        public static RaycastHit? TargetHit(bool checkPointerOverGameObject) {
            var ray = MainCamera.ScreenPointToRay(Input.mousePosition);

            if (checkPointerOverGameObject &&
                EventSystem.current.IsPointerOverGameObject()) {
                return null;
            }

            Array.Clear(HitResults, 0, 10);

            Physics.RaycastNonAlloc(
                ray,
                HitResults,
                Mathf.Infinity,
                Physics.DefaultRaycastLayers
            );

            return HitResults.Last(hit => hit.collider);
        }
    }
}

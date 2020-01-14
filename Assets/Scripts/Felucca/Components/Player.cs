using UnityEngine;

namespace Felucca.Components {
    public class Player : MonoBehaviour {
        private readonly RaycastHit[] _hitResults = new RaycastHit[10];
        private RaycastHit _lastHitResult;
        private Camera _camera;

        private void Start()
        {
            _camera = Camera.main;
        }

        private RaycastHit? TargetHit() {
            if (_camera == null) {
                return null;
            }

            var ray = _camera.ScreenPointToRay(Input.mousePosition);

            var size = Physics.RaycastNonAlloc(
                ray,
                _hitResults,
                Mathf.Infinity,
                Physics.DefaultRaycastLayers
            );

            foreach (var result in _hitResults)
            {
                if (result.collider != null)
                {
                    _lastHitResult = result;
                }
            }

            return _lastHitResult;
        }

        private static bool IsTerrainClick(RaycastHit hit) {
            return hit.transform.gameObject.name == "Terrain";
        }

        private void HandleTerrainClick(RaycastHit hit) {
            var point = hit.point;
            point.y = this.transform.localPosition.y;
            // TODO: Add a `creature` attribute?
            GetComponent<Creature>().SetDestination(point);
        }

        private void Update() {
            if (Input.GetKeyDown(KeyCode.Mouse1)) {
                var targetHit = TargetHit();
                if (targetHit == null) {
                    return;
                }
                if (IsTerrainClick(targetHit.Value)) {
                    HandleTerrainClick(targetHit.Value);
                }
            }
        }
    }
}
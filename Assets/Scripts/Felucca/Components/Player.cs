using System.Linq;
using UnityEngine;

namespace Felucca.Components {
    public class Player : MonoBehaviour {
        public Creature creature;

        private readonly RaycastHit[] _hitResults = new RaycastHit[10];
        private Camera _camera;
        private DoubleClickHandler _doubleClickHandler = new DoubleClickHandler();

        private void Start()
        {
            _camera = Camera.main;
            creature = GetComponent<Creature>();
        }

        private RaycastHit? TargetHit() {
            var ray = _camera.ScreenPointToRay(Input.mousePosition);

            Physics.RaycastNonAlloc(
                ray,
                _hitResults,
                Mathf.Infinity,
                Physics.DefaultRaycastLayers
            );

            return _hitResults.First();
        }

        private static bool IsCreatureClick(RaycastHit hit) {
            return hit.collider.gameObject.TryGetComponent(out Creature _);
        }

        private static bool IsTerrainClick(RaycastHit hit) {
            return hit.transform.gameObject.name == "Terrain";
        }

        private void HandleTerrainClick(RaycastHit hit) {
            creature.StopAttacking();
            var point = hit.point;
            point.y = this.transform.localPosition.y;
            creature.SetDestination(point);
        }

        private void HandleCreatureClick(RaycastHit hit) {
            var targetCreature = hit.collider.gameObject.GetComponent<Creature>();
            creature.StartAttacking(targetCreature);
            targetCreature.AcknowledgeAttacker(creature);
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
            } else if (_doubleClickHandler.DidDoubleClick()) {
                Debug.Log("double click");
                var targetHit = TargetHit();
                if (targetHit == null) {
                    return;
                }
                if (IsCreatureClick(targetHit.Value)) {
                    HandleCreatureClick(targetHit.Value);
                }
            }
        }
    }
}
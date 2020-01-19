using UnityEngine;

namespace Felucca.Components {
    public class Player : MonoBehaviour {
        public Creature creature;

        private void Start() {
            creature = GetComponent<Creature>();
        }

        private static bool IsCreatureClick(RaycastHit hit) {
            return hit.transform.gameObject.TryGetComponent(out Creature _);
        }

        private static bool IsTerrainClick(RaycastHit hit) {
            return hit.transform.gameObject.name == "Terrain";
        }

        private void HandleTerrainClick(RaycastHit hit) {
            var point = hit.point;
            point.y = this.transform.localPosition.y;
            creature.StartMovingTowards(point);
        }

        private void HandleCreatureClick(RaycastHit hit) {
            var targetCreature =
                hit.collider.gameObject.GetComponent<Creature>();
            creature.StartAttacking(targetCreature);
            targetCreature.AcknowledgeAttacker(creature);
        }

        private void Update() {
            if (Input.GetKey(KeyCode.Mouse1)) {
                var targetHit = TargetHitFinder.TargetHit(true);
                if (targetHit == null) {
                    return;
                }

                if (IsTerrainClick(targetHit.Value)) {
                    HandleTerrainClick(targetHit.Value);
                }
            } else if (DoubleClickHandler.DidDoubleClick()) {
                var targetHit = TargetHitFinder.TargetHit(true);
                if (targetHit == null) {
                    return;
                }

                if (IsCreatureClick(targetHit.Value)) {
                    HandleCreatureClick(targetHit.Value);
                }
            } else if (Input.GetKeyUp(KeyCode.Mouse1)) {
                creature.StopMoving();
            }
        }
    }
}

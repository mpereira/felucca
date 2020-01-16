using UnityEngine;

namespace Felucca.Components {
    public class DragCreatureBar : MonoBehaviour {
        public float dragStartThreshold;
        
        private Camera _camera;
        private CreatureBar _creatureBar;
        private Vector3? _startedDraggingFrom;
        
        private void Start() {
            _camera = Camera.main;
            _creatureBar = CreatureBar.Create(gameObject);
            
            dragStartThreshold = 10f;
        }
        
        private void OnMouseOver() {
        }
        
        private void OnMouseDown() {
            if (Input.GetKeyDown(KeyCode.Mouse0)) {
                _startedDraggingFrom = Input.mousePosition;
            }
        }

        private void OnMouseDrag() {
            if (_startedDraggingFrom == null) {
                return;
            }

            var dragDistance = Vector3.Distance(
                _startedDraggingFrom.Value,
                Input.mousePosition
            );
            
            if (dragDistance < dragStartThreshold) {
                return;
            }
            
            _creatureBar.ShowIfHidden();
            _creatureBar.MoveToFront();

            // FIXME: this is too expensive to run on every drag position.
            var targetHit = TargetHitFinder.TargetHit(false);
            if (targetHit == null) {
                return;
            }
            var screenPoint = _camera.WorldToScreenPoint(
                targetHit.Value.point
            );

            var position = new Vector3(
                screenPoint.x - Screen.width / 2f,
                screenPoint.y - Screen.height / 2f,
                0
            );
            
            _creatureBar.UpdatePosition(position);
        }

        private void OnMouseUp() {
            if (Input.GetKeyDown(KeyCode.Mouse0)) {
                _startedDraggingFrom = null;
            }
        }
    }
}
using UnityEngine;
using Image = UnityEngine.UI.Image;

namespace Felucca.Components {
    public class DragCreatureBar : MonoBehaviour {
        public Canvas canvas;
        public Creature creature;
        
        private Camera _camera;
        private CreatureBar _creatureBar;
        private readonly RaycastHit[] _hitResults = new RaycastHit[10];
        private Vector3? _startedDraggingFrom;
        private float _dragStartThreshold = 5f;
        
        private void Start() {
            _camera = Camera.main;
            _creatureBar = CreatureBar.Create(gameObject);
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
            
            if (dragDistance < _dragStartThreshold) {
                return;
            }
            
            _creatureBar.ShowIfHidden();

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
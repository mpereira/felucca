using UnityEngine;
using Image = UnityEngine.UI.Image;

namespace Felucca.Components {
    public class DragCreatureBar : MonoBehaviour {
        public Canvas canvas;
        public Creature creature;
        public GameObject creatureBarPanel;
        
        private Camera _camera;
        private GameObject _creatureBars;
        private readonly RaycastHit[] _hitResults = new RaycastHit[10];
        private Vector3? _startedDraggingFrom;
        private float _dragStartThreshold = 5f;
        
        private void Start() {
            _camera = Camera.main;
            _creatureBars = GameObject.Find("Creature Bars");
            
            creatureBarPanel = new GameObject(gameObject.name);
            creatureBarPanel.SetActive(false);
            creatureBarPanel.AddComponent<RectTransform>();
            creatureBarPanel.AddComponent<CanvasRenderer>();
            creatureBarPanel.AddComponent<Image>();
            creatureBarPanel.AddComponent<CreatureBar>();
            creatureBarPanel.transform.SetParent(_creatureBars.transform);
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
            if (!creatureBarPanel.activeSelf) {
                creatureBarPanel.SetActive(true);
            }

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
            creatureBarPanel.transform.localPosition = position;
        }

        private void OnMouseUp() {
            if (Input.GetKeyDown(KeyCode.Mouse0)) {
                _startedDraggingFrom = null;
            }
        }
    }
}
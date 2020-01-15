using System;
using UnityEditor;
using UnityEngine;
using UnityEngine.EventSystems;

namespace Felucca.Components {
    public class CreatureBar :
        MonoBehaviour,
        IDragHandler,
        IBeginDragHandler,
        IPointerEnterHandler,
        IPointerClickHandler,
        IEndDragHandler
    {
        public float dragThreshold;
        
        private Vector2 _lastMousePosition;

        private void Start() {
            dragThreshold = 20;
        }

        public void OnPointerEnter(PointerEventData eventData) {
        }

        public void OnPointerClick(PointerEventData eventData) {
            if (eventData.button == PointerEventData.InputButton.Right) {
                gameObject.SetActive(false);
            }
        }

        public void OnBeginDrag(PointerEventData eventData) {
            _lastMousePosition = eventData.position;
        }
     
        public void OnDrag(PointerEventData eventData) {
            Vector2 currentMousePosition = eventData.position;
            Vector2 diff = currentMousePosition - _lastMousePosition;
            RectTransform rect = GetComponent<RectTransform>();
     
            Vector3 newPosition = rect.position +  new Vector3(diff.x, diff.y, transform.position.z);
            Vector3 oldPos = rect.position;
            rect.position = newPosition;
            if(!IsRectTransformInsideScreen(rect)) {
                rect.position = oldPos;
            }
            _lastMousePosition = currentMousePosition;
        }
     
        public void OnEndDrag(PointerEventData eventData) {
        }
     
        private bool IsRectTransformInsideScreen(RectTransform rectTransform) {
            Vector3[] corners = new Vector3[4];
            rectTransform.GetWorldCorners(corners);
            int visibleCorners = 0;
            Rect rect = new Rect(0,0,Screen.width, Screen.height);
            foreach(Vector3 corner in corners) {
                if(rect.Contains(corner)) {
                    visibleCorners++;
                }
            }

            return visibleCorners == 4;
        }
    }
}

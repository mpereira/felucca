using System;
using UnityEditor;
using UnityEngine;
using UnityEngine.EventSystems;

namespace Felucca.Components {
    public class CreatureBar :
        MonoBehaviour,
        IDragHandler,
        IBeginDragHandler,
        IEndDragHandler
    {
        private Vector2 lastMousePosition;
        public float dragThreshold;

        private void Start() {
            dragThreshold = 20;
        }

        public void OnBeginDrag(PointerEventData eventData) {
            Debug.Log("Begin Drag");
            lastMousePosition = eventData.position;
        }
     
        public void OnDrag(PointerEventData eventData) {
            Vector2 currentMousePosition = eventData.position;
            Vector2 diff = currentMousePosition - lastMousePosition;
            RectTransform rect = GetComponent<RectTransform>();
     
            Vector3 newPosition = rect.position +  new Vector3(diff.x, diff.y, transform.position.z);
            Vector3 oldPos = rect.position;
            rect.position = newPosition;
            if(!IsRectTransformInsideSreen(rect))
            {
                rect.position = oldPos;
            }
            lastMousePosition = currentMousePosition;
        }
     
        public void OnEndDrag(PointerEventData eventData)
        {
            Debug.Log("End Drag");
            Debug.Log(eventData);
        }
     
        private bool IsRectTransformInsideSreen(RectTransform rectTransform) {
            bool isInside = false;
            Vector3[] corners = new Vector3[4];
            rectTransform.GetWorldCorners(corners);
            int visibleCorners = 0;
            Rect rect = new Rect(0,0,Screen.width, Screen.height);
            foreach(Vector3 corner in corners)
            {
                if(rect.Contains(corner))
                {
                    visibleCorners++;
                }
            }
            if(visibleCorners == 4)
            {
                isInside = true;
            }
            return isInside;
        }
    }
}

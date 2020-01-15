using System;
using TMPro;
using UnityEditor;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.Serialization;
using UnityEngine.UI;
using static UnityEngine.GameObject;

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
        public float width;
        public float height;
        public GameObject creatureBarPanel;
        public Creature creature;
        public Creature playerCreature;
        
        private Vector2 _lastMousePosition;
        private RectTransform _rectTransform;
        private Image _image;

        private void Awake() {
        }
        
        private void Start() {
            dragThreshold = 20;
            width = 80;
            height = 30;
            
            _rectTransform = GetComponent<RectTransform>();
            _rectTransform.sizeDelta = new Vector2(width, height);
            
            _image = GetComponent<Image>();
            var color = Color.white;
            color.a = 0.3f;
            _image.color = color;

            var text = gameObject.AddComponent<TextMeshPro>();
            // text.text = name;
        }

        public static CreatureBar Create(GameObject gameObject) {
            var creatureBarPanel = new GameObject(gameObject.name);
            var creatureBars = Find("Creature Bars");
            var playerCreature = Find("Player").GetComponent<Creature>();
            
            var creatureBar = creatureBarPanel.AddComponent<CreatureBar>();
            creatureBarPanel.AddComponent<RectTransform>();
            creatureBarPanel.AddComponent<CanvasRenderer>();
            creatureBarPanel.AddComponent<Image>();
            
            creatureBar.creatureBarPanel = creatureBarPanel;
            creatureBar.creature = gameObject.GetComponent<Creature>();
            creatureBar.playerCreature = playerCreature;
            
            creatureBar.Hide();
            
            creatureBarPanel.transform.SetParent(creatureBars.transform);
            
            return creatureBar;
        }

        public void Hide() {
            creatureBarPanel.SetActive(false);
        }

        public void Show() {
            creatureBarPanel.SetActive(true);
        }

        public bool IsShown() {
            return creatureBarPanel.activeSelf;
        }

        public bool IsHidden() {
            return !IsShown();
        }

        public void ShowIfHidden() {
            if (IsHidden()) {
                Show();
            }
        }

        public void UpdatePosition(Vector3 position) {
            transform.localPosition = position;
        }

        public void OnPointerEnter(PointerEventData eventData) {
        }

        public void OnPointerClick(PointerEventData eventData) {
            if (eventData.button == PointerEventData.InputButton.Right) {
                gameObject.SetActive(false);
            } else if (eventData.button == PointerEventData.InputButton.Left) {
                if (eventData.clickCount == 2) {
                    playerCreature.StartAttacking(creature);
                    creature.AcknowledgeAttacker(playerCreature);
                }
            }
        }

        public void OnBeginDrag(PointerEventData eventData) {
            _lastMousePosition = eventData.position;
        }
     
        public void OnDrag(PointerEventData eventData) {
            Vector2 currentMousePosition = eventData.position;
            Vector2 diff = currentMousePosition - _lastMousePosition;
            Vector3 newPosition = _rectTransform.position + new Vector3(
                diff.x, diff.y, transform.position.z
            );
            Vector3 oldPos = _rectTransform.position;
            _rectTransform.position = newPosition;
            if(!IsRectTransformInsideScreen()) {
                _rectTransform.position = oldPos;
            }
            _lastMousePosition = currentMousePosition;
        }
     
        public void OnEndDrag(PointerEventData eventData) {
        }
     
        private bool IsRectTransformInsideScreen() {
            Vector3[] corners = new Vector3[4];
            _rectTransform.GetWorldCorners(corners);
            int visibleCorners = 0;
            Rect rect = new Rect(0, 0, Screen.width, Screen.height);
            foreach (Vector3 corner in corners) {
                if (rect.Contains(corner)) {
                    visibleCorners++;
                }
            }

            return visibleCorners == 4;
        }
    }
}

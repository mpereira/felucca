using System;
using TMPro;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.Serialization;
using static UnityEngine.GameObject;
using Image = UnityEngine.UI.Image;

namespace Felucca.Components {
    public class CreatureBar :
        MonoBehaviour,
        IDragHandler,
        IBeginDragHandler,
        IPointerEnterHandler,
        IPointerClickHandler,
        IEndDragHandler
    {
        public float width;
        public float height;
        public GameObject creatureBarContainer;
        public Creature creature;
        public Creature playerCreature;
        
        public Image hitPointsImage;
        public RectTransform rectTransform;
        
        private Vector2 _lastMousePosition;

        public CreatureBar() {
            width = 80;
            height = 30;
        }
        
        private void Awake() {
            gameObject.AddComponent<CanvasRenderer>();
            rectTransform = gameObject.AddComponent<RectTransform>();
            rectTransform.sizeDelta = new Vector2(width, height);
        }

        public static CreatureBar Create(GameObject gameObject) {
            var creatureBars = Find("Creature Bars");
            var playerCreature = Find("Player").GetComponent<Creature>();
            
            var creatureBarContainer = new GameObject(gameObject.name);
            var creatureBar = creatureBarContainer.AddComponent<CreatureBar>();
            creatureBarContainer.transform.SetParent(creatureBars.transform);

            var sizeDelta = new Vector2(creatureBar.width, creatureBar.height);
            
            var creatureBarBackground = new GameObject("Background");
            creatureBarBackground.transform.SetParent(creatureBarContainer.transform);
            var backgroundImageColor = Color.white;
            backgroundImageColor.a = 0.5f;
            // The Image component comes with a RectTransform.
            var backgroundImage = creatureBarBackground.AddComponent<Image>();
            backgroundImage.color = backgroundImageColor;
            var backgroundRectTransform = creatureBarBackground.GetComponent<RectTransform>();
            backgroundRectTransform.sizeDelta = sizeDelta;
            
            var creatureBarText = new GameObject("Text");
            creatureBarText.transform.SetParent(creatureBarContainer.transform);
            // The TextMeshProUGUI component comes with a RectTransform.
            var text = creatureBarText.AddComponent<TextMeshProUGUI>();
            text.text = gameObject.name;
            text.alignment = TextAlignmentOptions.TopGeoAligned;
            text.fontSize = 12f;
            var textRectTransform = creatureBarText.GetComponent<RectTransform>();
            textRectTransform.sizeDelta = sizeDelta;
            
            var creatureBarHitPoints = new GameObject("Hit Points");
            creatureBarHitPoints.transform.SetParent(creatureBarContainer.transform);
            var hitPointsImage = creatureBarHitPoints.AddComponent<Image>();
            var hitPointsRectTransform = creatureBarHitPoints.GetComponent<RectTransform>();
            hitPointsImage.color = Color.red;
            // http://www.1x1px.me/.
            hitPointsImage.sprite = Resources.Load<Sprite>("FFFFFF-1");
            hitPointsImage.type = Image.Type.Filled;
            hitPointsImage.fillMethod = Image.FillMethod.Horizontal;
            hitPointsRectTransform.sizeDelta = new Vector2(sizeDelta.x * 0.8f, 8);
            hitPointsRectTransform.position = new Vector3(0, 8, 0);
            // Anchor bottom center works nice with sizeDelta.y == position.
            hitPointsRectTransform.anchorMax = new Vector2(0.5f, 0);
            hitPointsRectTransform.anchorMin = new Vector2(0.5f, 0);
            
            creatureBar.creatureBarContainer = creatureBarContainer;
            creatureBar.creature = gameObject.GetComponent<Creature>();
            creatureBar.playerCreature = playerCreature;
            creatureBar.hitPointsImage = hitPointsImage;
            
            creatureBar.Hide();
            
            return creatureBar;
        }

        public float CreatureHitPointPercentage() {
            return (float) creature.hitPoints / creature.MaxHitPoints();
        }

        private void Update() {
            hitPointsImage.fillAmount = CreatureHitPointPercentage();
        }

        public void Hide() {
            creatureBarContainer.SetActive(false);
        }

        public void Show() {
            creatureBarContainer.SetActive(true);
        }
        
        public void MoveToFront() {
            rectTransform.SetAsLastSibling();
        }

        public bool IsShown() {
            return creatureBarContainer.activeSelf;
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
                Hide();
            } else if (eventData.button == PointerEventData.InputButton.Left) {
                if (eventData.clickCount == 2) {
                    playerCreature.StartAttacking(creature);
                    creature.AcknowledgeAttacker(playerCreature);
                } else if (eventData.clickCount == 1) {
                    MoveToFront();
                }
            }
        }

        public void OnBeginDrag(PointerEventData eventData) {
            _lastMousePosition = eventData.position;
            MoveToFront();
        }
     
        public void OnDrag(PointerEventData eventData) {
            Vector2 currentMousePosition = eventData.position;
            Vector2 diff = currentMousePosition - _lastMousePosition;
            Vector3 newPosition = rectTransform.position + new Vector3(
                diff.x, diff.y, transform.position.z
            );
            Vector3 oldPos = rectTransform.position;
            rectTransform.position = newPosition;
            if(!IsRectTransformInsideScreen()) {
                rectTransform.position = oldPos;
            }
            _lastMousePosition = currentMousePosition;
        }
     
        public void OnEndDrag(PointerEventData eventData) {
        }
     
        private bool IsRectTransformInsideScreen() {
            Vector3[] corners = new Vector3[4];
            rectTransform.GetWorldCorners(corners);
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

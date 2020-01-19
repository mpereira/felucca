using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using TMPro;
using UnityEngine;

namespace Felucca.Components {
    public class BottomLeftMessages : MonoBehaviour {
        public StatAndSkillSystem statAndSkillSystem;
        public GameObject         bottomLeftMessages;
        public GameObject         bottomLeftMessagePrefab;
        public float              marginLeft;
        public float              marginBottom;
        public int                messageTtl;
        public float              messageDisappearSpeed;
        public float              messageFontSize;
        public float              messageVerticalMargin;
        public LinkedList<string> messages = new LinkedList<string>();
        public int                maximumMessages;

        private float    _calculatedFontMarginBottom;
        private TextInfo _textInfo;

        private readonly Dictionary<string, GameObject> _messageGameObjects =
            new Dictionary<string, GameObject>();

        private void Start() {
            statAndSkillSystem.OnStatChange += DisplayStatChange;
            statAndSkillSystem.OnSkillChange += DisplaySkillChange;

            _textInfo = CultureInfo.CurrentCulture.TextInfo;
            _calculatedFontMarginBottom = messageFontSize / 2f;
        }

        public void RegisterStatAndSkillSystem(
            StatAndSkillSystem statAndSkillSystem
        ) {
            this.statAndSkillSystem = statAndSkillSystem;
        }

        private IEnumerable<RectTransform> TextMessageRectTransforms() {
            return GetComponentsInChildren<RectTransform>().Where(
                c => c.TryGetComponent<TextMessage>(out _)
            );
        }

        private void ShiftAllMessagesUp() {
            var textMessageRectTransforms = TextMessageRectTransforms();
            foreach (
                var rectTransform in textMessageRectTransforms
            ) {
                var anchoredPosition = rectTransform.anchoredPosition;
                var newF = new Vector2(
                    anchoredPosition.x,
                    anchoredPosition.y + NewMessageHeight()
                );
                rectTransform.anchoredPosition = newF;
            }
        }

        private void AddMessage(string message) {
            if (messages.Count == maximumMessages) {
                RemoveMessage(messages.Last());
            }

            ShiftAllMessagesUp();

            messages.AddFirst(message);

            StartCoroutine(RemoveMessageAfterTtl(message));
        }

        private IEnumerator RemoveMessageAfterTtl(string message) {
            yield return new WaitForSeconds(messageTtl + messageDisappearSpeed);

            RemoveMessage(message);
        }

        private void RemoveMessage(string message) {
            messages.Remove(message);
            if (_messageGameObjects.TryGetValue(
                message,
                out var messageGameObject
            )) {
                Destroy(messageGameObject);
            }
        }

        private float NewMessageHeight() {
            return messageFontSize + messageVerticalMargin;
        }

        private float NewMessageYOffset() {
            return _calculatedFontMarginBottom + marginBottom;
        }

        private void DisplayMessage(string message) {
            var messageGameObject = Instantiate(
                bottomLeftMessagePrefab,
                bottomLeftMessages.transform,
                true
            );

            _messageGameObjects.Add(message, messageGameObject);

            var fadingVisual = messageGameObject.GetComponent<FadingVisual>();
            fadingVisual.SetTtlAndDisappearSpeed(
                messageTtl,
                messageDisappearSpeed
            );

            var messageText = messageGameObject.GetComponent<TextMeshProUGUI>();
            messageText.text = message;
            messageText.fontSize = messageFontSize;

            var rectTransform = messageGameObject.GetComponent<RectTransform>();
            rectTransform.sizeDelta = new Vector2(0, NewMessageHeight());
            rectTransform.anchoredPosition = new Vector2(
                marginLeft,
                NewMessageYOffset()
            );
        }

        private void DisplayStatChange(String stat, int value, int increase) {
            var message =
                "Your " + stat + " changed by " + increase + ". " +
                "It is now " + value + ".";
            AddMessage(message);
            DisplayMessage(message);
        }

        private void DisplaySkillChange(
            string skill, double value, double increase
        ) {
            var message =
                "Your skill in " + _textInfo.ToTitleCase(skill) +
                " has increased by " + increase + ". " + "It is now " + value +
                "%.";
            AddMessage(message);
            DisplayMessage(message);
        }
    }
}

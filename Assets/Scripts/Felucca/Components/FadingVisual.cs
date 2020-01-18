using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace Felucca.Components {
    public class FadingVisual : MonoBehaviour {
        public float   ttl;
        public float   disappearSpeed;
        public Graphic graphic;

        private float _disappearTime;
        private Color _color;

        private void Awake() {
            _disappearTime = ttl;
            _color = graphic.color;
        }

        public void SetTtlAndDisappearSpeed(float ttl, float disappearSpeed) {
            this.ttl = ttl;
            this.disappearSpeed = disappearSpeed;
        }

        private void Update() {
            _disappearTime -= Time.deltaTime;
            if (_disappearTime <= 0) {
                // Dividing `disappearSpeed` by 5 to make it represent seconds
                // more closely.
                _color.a -= disappearSpeed * Time.deltaTime;
                if (_color.a > 0) {
                    graphic.color = _color;
                }
            }
        }
    }
}

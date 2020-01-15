using UnityEngine;
using UnityEngine.UI;

namespace Felucca {
    public class Core : MonoBehaviour {
        public Canvas canvas;
        
        void Start() {
            canvas = GetComponent<Canvas>();
            var raycaster = canvas.GetComponent<GraphicRaycaster>();
        }
    }
}

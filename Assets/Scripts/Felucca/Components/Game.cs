using Felucca.Components;
using UnityEngine;

namespace Felucca {
    public class Game : MonoBehaviour {
        public Camera mainCamera;
        public Light mainLight;
        public Player player;
        
        void Awake() {
            player = FindObjectOfType<Player>();
            mainLight = FindObjectOfType<Light>();
            mainCamera = Camera.main;
            
            mainLight.transform.localRotation = Quaternion.Euler(45, -30, 0);
            mainLight.type = LightType.Directional;
            mainLight.intensity = 1.5f;
            mainLight.shadows = LightShadows.Soft;
            
            var followingCamera = mainCamera.gameObject.AddComponent<FollowingCamera>();
            followingCamera.followee = player.gameObject;
        }
    }
}

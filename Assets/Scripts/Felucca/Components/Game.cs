using Felucca.Components;
using UnityEngine;

namespace Felucca {
    public class Game : MonoBehaviour {
        public Camera mainCamera;
        public Light mainLight;
        
        public GameObject playerPrefab;
        public GameObject ratmanPrefab;
        public GameObject dragonPrefab;
        
        public Player player;
        public Creature ratman;
        public Creature dragon;
        
        void Awake() {
            player = Instantiate(playerPrefab).GetComponent<Player>();
            player.name = "Player";
            ratman = Instantiate(ratmanPrefab).GetComponent<Creature>();
            ratman.name = "a ratman";
            dragon = Instantiate(dragonPrefab).GetComponent<Creature>();
            dragon.name = "a dragon";
            
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

(ns felucca.core
  (:use arcadia.core)
  (:require [arcadia.introspection :as introspection]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :as repl]
            [felucca.vector3 :as v3]
            [felucca.utils :as utils]
            [felucca.components.creature :as creature]
            [felucca.components.hit-points-bar :as hit-points-bar]
            [felucca.creature-spec :as creature-spec]
            [felucca.creature-specs :as creature-specs]
            [felucca.components.following-camera :as following-camera]
            [felucca.components.player-input :as player-input])
  (:import (UnityEngine
            Application
            Camera
            CharacterController
            Collision
            Color
            Debug
            GameObject
            Input
            KeyCode
            Light
            LightType
            Material
            Mathf
            MeshRenderer
            MonoBehaviour
            Physics
            Plane
            Quaternion
            Ray
            RaycastHit
            Rect
            Renderer
            Resources
            Shader
            Space
            Terrain
            Texture
            Texture2D
            Time
            Transform
            Vector3
            WaitForSeconds)))

(defn create-creature [{creature-name :name :as creature-state}
                       {:keys [color scale] :as ui}
                       position]
  (let [hit-points-bar-texture (Texture2D. 1 1 (TextureFormat/RGB24) false)
        hit-points-bar-texture-background (Texture2D. 1 1 (TextureFormat/RGB24) false)
        _ (.SetPixel hit-points-bar-texture-background 0 0 (Color/black))
        _ (.Apply hit-points-bar-texture-background)
        _ (.SetPixel hit-points-bar-texture 0 0 (Color/red))
        _ (.Apply hit-points-bar-texture)
        hit-points-bar-state (hit-points-bar/->HitPointsBar
                              hit-points-bar-texture-background
                              hit-points-bar-texture)
        creature-material (Material. (Shader/Find "Specular"))
        _ (set! (.color creature-material) color)
        creature (create-primitive :cube creature-name)]
    (with-cmpt creature [r Renderer]
      (set! (.material r) creature-material))
    (with-cmpt creature [t Transform]
      (set! (.localPosition t) position)
      (set! (.localRotation t) (Quaternion/Euler 0 0 0))
      (set! (.localScale t) scale))
    (with-cmpt creature [cc CharacterController]
      (set! (. cc radius) 0.5))
    (utils/install-hooks creature :creature creature/hooks)
    (state+ creature :creature creature-state)
    (utils/install-hooks creature :hit-points-bar hit-points-bar/hooks)
    (state+ creature :hit-points-bar hit-points-bar-state)
    creature))

(defn create-player [creature-state* position]
  (let [player (create-creature creature-state*
                                {:scale (v3/v3 1 2 1)
                                 :color (Color/blue)}
                                position)
        player-input-state (player-input/->PlayerInput)]
    (utils/install-hooks player :player-input player-input/hooks)
    (state+ player :player-input player-input-state)
    player))

(defn create-terrain []
  (let [terrain (create-primitive :plane "Terrain")
        grass-material (Resources/Load "Ground textures pack/Grass 05/Grass pattern 05")]
    (with-cmpt terrain [t Transform]
      (set! (.localPosition t) (v3/v3 0 0 0))
      (set! (.localRotation t) (Quaternion/Euler 0 0 0))
      (set! (.localScale t) (v3/v3 20 1 20)))
    (with-cmpt terrain [t Terrain])
    (with-cmpt terrain [mr MeshRenderer]
      (set! (.material mr) grass-material))
    terrain))

(defn create-light []
  (let [light (GameObject. "Light")]
    (with-cmpt light [t Transform]
      (set! (.localPosition t) (v3/v3 0 5 0))
      (set! (.localRotation t) (Quaternion/Euler 45 0 0)))
    (with-cmpt light [l Light]
      (set! (.type l) LightType/Directional)
      (set! (.intensity l) 0.4)
      (set! (.color l) Color/white))
    light))

(defn set-up-main-camera [player]
  (let [main-camera (utils/find-object "Main Camera")]
    (with-cmpt main-camera [t Transform]
      (set! (.localPosition t) (v3/v3 30 25 -30))
      (set! (.localEulerAngles t) (v3/v3 30 -45 0)))
    (with-cmpt main-camera [c Camera]
      (set! (.orthographic c) true)
      (set! (.orthographicSize c) 10)
      (set! (.backgroundColor c) Color/clear)
      (set! (.nearClipPlane c) -100.0))
    (utils/install-hooks main-camera :following-camera following-camera/hooks)
    (state+ main-camera :following-camera (following-camera/->FollowingCamera
                                           player (v3/vempty)))))

(defn start []
  (def player (create-player (creature/->Creature "Player"
                                                  25
                                                  25
                                                  10
                                                  0
                                                  0
                                                  0
                                                  50
                                                  50
                                                  30
                                                  100
                                                  (v3/vempty)
                                                  (v3/vempty)
                                                  nil
                                                  0.0
                                                  [])
                             (v3/v3 -5 1 -5)))
  (def rat (create-creature (creature-spec/spec->creature creature-specs/rat)
                            (:ui creature-specs/rat)
                            (v3/v3 8 0.5 8)))
  (def dragon (create-creature (creature-spec/spec->creature creature-specs/dragon)
                               (:ui creature-specs/dragon)
                               (v3/v3 12 1 12)))
  (def terrain (create-terrain))
  (def light (create-light))
  (def main-camera (set-up-main-camera player)))

(defn stop []
  (let [object-names ["a dragon"
                      "a rat"
                      "Light"
                      "Player"
                      "Terrain"]]
    (doseq [object (mapcat objects-named object-names)]
      (GameObject/DestroyImmediate object))))

(defn reset []
  (stop)
  (start))

(comment
  (in-ns 'felucca.core)

  (reset)

  (stop)

  (last (introspection/methods-report MeshRenderer))
  (pprint (introspection/methods Material))
  (pprint (introspection/methods Resources))
  (pprint (introspection/fields Shader))

  (let [asset-ids (AssetDatabase/FindAssets "Grass")]
    (for [asset-id asset-ids]
      (AssetDatabase/GUIDToAssetPath asset-id)))
  (AssetDatabase/LoadAssetAtPath (type-args Texture2D) "some-path")

  (cmpt terrain Terrain)

  (start)

  (available-hooks)

  (utils/after 3 #(Debug/Log "Ta-daaaaaaa!!!!"))

  (def colors [Color/black
               Color/blue
               Color/cyan
               Color/gray
               Color/green
               Color/magenta
               Color/red
               Color/white
               Color/yellow])

  (doseq [[color index] (map vector colors (iterate (partial + 2) 2))]
    (after index #(set! (.color terrain-material) color))))

(ns battle-arena.core
  (:use arcadia.core)
  (:require [battle-arena.vector3 :refer :all]
            [battle-arena.utils :as utils]
            [battle-arena.components.hero :as hero]
            [battle-arena.components.enemy :as enemy]
            [battle-arena.components.rts-camera :as rts-camera]
            [battle-arena.components.player-input :as player-input])
  (:import
   (battle-arena.components.player-input.PlayerInput)
   (UnityEngine
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
    MonoBehaviour
    Physics
    Plane
    Quaternion
    Ray
    RaycastHit
    Rect
    Renderer
    Shader
    Space
    Time
    Transform
    Vector3
    WaitForSeconds)))

(defn install-hooks [game-object role-key hooks]
  (doseq [[hook hook-fn] hooks]
    (hook+ game-object hook role-key hook-fn)))

(defn create-hero [{:keys [x z name strength attack-speed attack-range
                           hit-points movement-speed rotation-speed]}]
  (let [hero (create-primitive :cube)
        hero-state (hero/->Hero strength
                                hit-points
                                attack-speed
                                attack-range
                                movement-speed
                                rotation-speed
                                nil
                                nil)]
    (set! (. hero name) name)
    (with-cmpt hero [t Transform]
      (set! (.localPosition t) (v3 x (utils/height hero) z))
      (set! (.localRotation t) (Quaternion/Euler 0 0 0))
      (set! (.localScale t) (v3 1 2 1)))
    (with-cmpt hero [cc CharacterController]
      (set! (. cc radius) 0.5))
    (install-hooks hero :hero hero/hooks)
    (state+ hero :hero hero-state)
    hero))

(defn create-enemy-hero [{:keys [aggressiveness-radius player-hero]
                          :as attributes}]
  (let [enemy-hero (create-hero attributes)]
    (install-hooks enemy-hero :enemy enemy/hooks)
    (state+ enemy-hero :enemy (enemy/->Enemy player-hero aggressiveness-radius))
    enemy-hero))

(defn create-player [attributes]
  (let [player (create-hero attributes)
        player-input-state (player-input/->PlayerInput nil
                                                       (v3 0 0 0)
                                                       nil
                                                       nil
                                                       0.0)]
    (install-hooks player :player-input player-input/hooks)
    (state+ player :player-input player-input-state)
    player))

(defn create-terrain []
  (let [terrain (create-primitive :plane)]
    (set! (. terrain name) "Terrain")
    (with-cmpt terrain [t Transform]
      (set! (.localPosition t) (v3 0 0 0))
      (set! (.localRotation t) (Quaternion/Euler 0 0 0))
      (set! (.localScale t) (v3 20 1 20)))
    terrain))

(defn start []
  ;;;;;;;;;;;;
  ;; Player ;;
  ;;;;;;;;;;;;

  (def player-hero-material (Material. (Shader/Find "Specular")))

  (set! (.color player-hero-material) Color/blue)

  (def player-hero (create-player {:name "Player Hero"
                                   :x -5
                                   :z -5
                                   :strength 25
                                   :hit-points 25
                                   :attack-speed 2.0
                                   :attack-range 1.2
                                   :movement-speed 10.0
                                   :rotation-speed 500.0}))

  (with-cmpt player-hero [r Renderer]
    (set! (.material r) player-hero-material))

  ;;;;;;;;;;;
  ;; Enemy ;;
  ;;;;;;;;;;;

  (def enemy-hero-material (Material. (Shader/Find "Specular")))

  (set! (.color enemy-hero-material) Color/red)

  (def enemy-hero (create-enemy-hero {:name "Enemy Hero"
                                      :x 5
                                      :z 5
                                      :strength 15
                                      :hit-points 15
                                      :attack-speed 4
                                      :attack-range 1.2
                                      :movement-speed 15
                                      :rotation-speed 150
                                      :player-hero player-hero
                                      :aggressiveness-radius 10.0}))

  (with-cmpt enemy-hero [r Renderer]
    (set! (.material r) enemy-hero-material))

  ;;;;;;;;;;;;;
  ;; Terrain ;;
  ;;;;;;;;;;;;;

  (def terrain-material (Material. (Shader/Find "Specular")))

  (set! (.color terrain-material) (Color. 0.4 0.8 0.2))

  (def terrain (create-terrain))

  (with-cmpt terrain [r Renderer]
    (set! (.material r) terrain-material))

  ;;;;;;;;;;;
  ;; Light ;;
  ;;;;;;;;;;;

  (def light (GameObject. "Light"))

  (with-cmpt light [t Transform]
    (set! (.localPosition t) (v3 0 5 0))
    (set! (.localRotation t) (Quaternion/Euler 45 0 0)))

  (with-cmpt light [l Light]
    (set! (.type l) LightType/Directional)
    (set! (.intensity l) 0.4)
    (set! (.color l) Color/white))

  ;;;;;;;;;;;;
  ;; Camera ;;
  ;;;;;;;;;;;;

  (def main-camera (utils/find-object "Main Camera"))

  (with-cmpt main-camera [t Transform]
    (set! (.localPosition t) (v3 30 25 -30))
    (set! (.localEulerAngles t) (v3 30 -45 0)))

  (with-cmpt main-camera [c Camera]
    (set! (.orthographic c) true)
    (set! (.orthographicSize c) 10)
    (set! (.backgroundColor c) Color/clear))

  (install-hooks main-camera :rts-camera rts-camera/hooks)
  (state+ main-camera :rts-camera (rts-camera/->RTSCamera 10 100)))

(defn stop []
  (let [object-names ["Enemy Hero"
                      "Player Hero"
                      "Light"
                      "Terrain"]]
    (doseq [object (mapcat objects-named object-names)]
      (GameObject/DestroyImmediate object))))

(defn reset []
  (stop)
  (start))

(comment
  (in-ns 'battle-arena.core)

  (reset)

  (stop)

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

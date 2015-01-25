(ns src.battle-arena.core
  (:use arcadia.core
        src.battle-arena.vector3)
  (:require [arcadia.hydrate :as h]
            [src.battle-arena.components.hero]
            [src.battle-arena.components.rts-camera]
            [src.battle-arena.components.click-to-move])
  (:import [UnityEngine
            Application
            Camera
            Collision
            Color
            Debug
            GameObject
            Input
            KeyCode
            LightType
            Material
            Mathf
            Physics
            Plane
            Ray
            RaycastHit
            Rect
            Shader
            Space
            Time
            Transform
            Vector3]))

;; FIXME: trying to `import` these result in "System.NullReferenceException:
;; Object reference not set to an instance of an object"
(def Hero        src.battle_arena.components.hero.Hero)
(def RTSCamera   src.battle_arena.components.rts_camera.RTSCamera)
(def ClickToMove src.battle_arena.components.click_to_move.ClickToMove)

(defn find-object [s] (. GameObject (Find s)))

(defn main-camera [] (UnityEngine.Camera/main))

(defn height [object] (.. object renderer bounds size y))

(defn create-hero [movement-speed]
  (let [hero (-> (create-primitive :cube)
                 (#(h/populate!
                     %
                     {:name "Hero"
                      :transform [{:local-position [0 (height %) 0]
                                   :local-rotation (Quaternion/Euler 0 0 0)
                                   :local-scale [1 2 1]}]
                      :character-controller [{}]}))
                 (add-component Hero))]
    (set! (.. hero (GetComponent Hero) movement-speed) (float movement-speed))
    hero))

(defn create-terrain []
  (-> (create-primitive :plane)
      (#(h/populate!
          %
          {:name "Terrain"
           :transform [{:local-position [0 0 0]
                        :local-rotation [0 0 0 0]
                        :local-scale [5 1 5]}]}))))

(defn start []
  #_(Debug/Log "starting")
  #_(create-hero)
  #_(create-terrain))

(defn stop []
  #_(Debug/Log "stopping")
  (dorun (map destroy (objects-named "Hero")))
  (dorun (map destroy (objects-named "Light")))
  (dorun (map destroy (objects-named "Terrain"))))

(defn reset []
  (stop)
  (destroy (.. (main-camera) (GetComponent RTSCamera)))
  #_(start))

(reset)

(def hero-material (Material. (Shader/Find "Specular")))
(set! (.color hero-material) Color/blue)

(def terrain-material (Material. (Shader/Find "Specular")))
(set! (.color terrain-material) Color/green)

(def hero (create-hero 10))
(set! (.. hero renderer material) hero-material)

(def terrain (create-terrain))
(set! (.. terrain renderer material) terrain-material)

(def light (GameObject. "Light"))
(add-component light "Light")

(h/populate! light {:transform [{:local-position [0 5 0]
                                 :local-rotation (Quaternion/Euler 45 0 0)}]
                    :light [{:type LightType/Directional
                             :intensity 0.4
                             :color Color/white}]})

(set! (.. terrain renderer material) terrain-material)

(set! (.. (main-camera) transform localPosition) (v3 30 25 -30))
(set! (.. (main-camera) transform localEulerAngles) (v3 30 -45 0))
(h/populate! (main-camera) {:orthographic true
                            :orthographic-size 10
                            :background-color Color/clear})

(add-component (.. (main-camera) gameObject) RTSCamera)
(set! (.. (main-camera) (GetComponent RTSCamera) speed) (float 10))
(set! (.. (main-camera) (GetComponent RTSCamera) border) (float 100))

(add-component (.. hero gameObject) ClickToMove)
(set! (.. hero (GetComponent ClickToMove) camera) (main-camera))
(set! (.. hero (GetComponent ClickToMove) movement-speed) (float 30))
(set! (.. hero (GetComponent ClickToMove) rotation-speed) (float 200))

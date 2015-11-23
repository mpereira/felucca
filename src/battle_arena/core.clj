(ns src.battle-arena.core
  (:use arcadia.core)
  (:require [src.battle-arena.vector3 :refer :all]
            [src.battle-arena.utils :refer :all]
            [arcadia.hydrate :as h]
            [src.battle-arena.components.hero :as hero]
            [src.battle-arena.components.enemy :as enemy]
            [src.battle-arena.components.rts-camera :as rts-camera]
            [src.battle-arena.components.player-input :as player-input])
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
            MonoBehaviour
            Physics
            Plane
            Ray
            RaycastHit
            Rect
            Shader
            Space
            Time
            Transform
            Vector3
            WaitForSeconds]))

(defn create-hero [{:keys [name strength attack-speed]}]
  (let [hero (create-primitive :cube)]
    (h/populate!
      hero
      {:name name
       :transform [{:local-position [0 (height hero) 0]
                    :local-rotation (Quaternion/Euler 0 0 0)
                    :local-scale [1 2 1]}]
       :character-controller [{:radius 0.5}]})
    (.AddComponent hero hero/Component)
    (set! (.. hero (GetComponent hero/Component) strength) strength)
    (set! (.. hero (GetComponent hero/Component) attack-speed) attack-speed)
    hero))

(defn create-enemy-hero [{:keys [movement-speed rotation-speed player-hero
                                 aggressiveness-radius]
                          :as attributes}]
  (let [enemy-hero (create-hero (select-keys attributes [:name :strength
                                                         :attack-speed]))]
    (.AddComponent enemy-hero enemy/Component)
    (set! (.. enemy-hero (GetComponent enemy/Component) player-hero) player-hero)
    (set! (.. enemy-hero (GetComponent enemy/Component) aggressiveness-radius)
          aggressiveness-radius)
    enemy-hero))

(defn create-player [{:keys [camera movement-speed rotation-speed] :as attributes}]
  (let [player (create-hero (select-keys attributes [:name :strength
                                                     :attack-speed]))]
    (.AddComponent player player-input/Component)
    (set! (.. player (GetComponent hero/Component) movement-speed)
          movement-speed)
    (set! (.. player (GetComponent hero/Component) rotation-speed)
          rotation-speed)
    player))

(defn create-terrain []
  (-> (create-primitive :plane)
      (#(h/populate!
          %
          {:name "Terrain"
           :transform [{:local-position [0 0 0]
                        :local-rotation [0 0 0 0]
                        :local-scale [5 1 5]}]}))))

(defn start []
  (def player-hero-material (Material. (Shader/Find "Specular")))

  (set! (.color player-hero-material) Color/blue)

  (def enemy-hero-material (Material. (Shader/Find "Specular")))

  (set! (.color enemy-hero-material) Color/red)

  (def terrain-material (Material. (Shader/Find "Specular")))

  (set! (.color terrain-material) (Color. 0.4 0.8 0.2))

  (def player-hero (create-player {:name "Player Hero"
                                   :movement-speed 30
                                   :rotation-speed 200
                                   :attack-speed 2
                                   :strength 25
                                   :camera (main-camera)}))

  (def enemy-hero (create-enemy-hero {:name "Enemy Hero"
                                      :movement-speed 15
                                      :rotation-speed 150
                                      :attack-speed 4
                                      :strength 15
                                      :player-hero player-hero
                                      :aggressiveness-radius 10}))

  (set! (.. player-hero renderer material) player-hero-material)

  (set! (.. enemy-hero renderer material) enemy-hero-material)

  (def terrain (create-terrain))
  (set! (.. terrain renderer material) terrain-material)

  (def light (GameObject. "Light"))
  (add-component light "Light")

  (h/populate! light {:transform [{:local-position [0 5 0]
                                   :local-rotation (Quaternion/Euler 45 0 0)}]
                      :light [{:type LightType/Directional
                               :intensity 0.4
                               :color Color/white}]})

  (set! (.. (find-object "Terrain") renderer material) terrain-material)

  (set! (.. (main-camera) transform localPosition) (v3 30 25 -30))
  (set! (.. (main-camera) transform localEulerAngles) (v3 30 -45 0))
  (h/populate! (main-camera) {:orthographic true
                              :orthographic-size 10
                              :background-color Color/clear})

  (add-component (.. (main-camera) gameObject) rts-camera/Component)
  (set! (.. (main-camera) (GetComponent rts-camera/Component) speed) (float 10))
  (set! (.. (main-camera) (GetComponent rts-camera/Component) border) (float 100)))

(defn stop []
  (dorun (map destroy (objects-named "Enemy Hero")))
  (dorun (map destroy (objects-named "Player Hero")))
  (dorun (map destroy (objects-named "Light")))
  (dorun (map destroy (objects-named "Terrain")))
  (doseq [rts-camera (.. (main-camera) (GetComponents rts-camera/Component))]
    (destroy rts-camera)))

(defn reset []
  (stop)
  (start))

(comment

 (in-ns 'src.battle-arena.core)

 (stop)

 (start)

 (after 3 #(Debug/Log "Ta-daaaaaaa!!!!"))

 (def colors [Color/black
              Color/blue
              Color/cyan
              Color/gray
              Color/green
              Color/magenta
              Color/red
              Color/white
              Color/yellow])

 (map vector colors (iterate (partial + 2) 2))

 (doseq [[color index] (map vector colors (iterate (partial + 2) 2))]
   (after index #(set! (.color terrain-material) color))))

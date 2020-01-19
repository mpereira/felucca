using System;
using UnityEngine;
using UnityEngine.Assertions;
using UnityEngine.UIElements;

namespace Felucca.Components {
    public class AttackPopup : MonoBehaviour {
        public float movementSpeed;

        private Creature _creature;
        private Vector3  _creatureSizeOffset;
        private bool     _hasCreature;
        private Camera   _mainCamera;
        private Vector3  _offset;

        public void Follow(Creature creature) {
            this._creature = creature;
            this._hasCreature = true;
            this._creatureSizeOffset = CreatureSizeOffset(creature);
        }

        private void Awake() {
            _mainCamera = Camera.main;
        }

        private Vector3 CreatureScreenPosition() {
            return _mainCamera.WorldToScreenPoint(_creature.transform.position);
        }

        private Vector3 CreatureSizeOffset(Creature creature) {
            var creatureSize = creature.GetComponent<Renderer>().bounds.size;
            return new Vector3(
                0,
                creatureSize.y * 4,
                0
            );
        }

        private Vector3 Position() {
            return CreatureScreenPosition() +
                   _creatureSizeOffset +
                   (_offset * movementSpeed);
        }

        public void LateUpdate() {
            if (!_hasCreature) {
                return;
            }

            _offset += Vector3.up * Time.deltaTime;

            transform.position = Position();
        }
    }
}

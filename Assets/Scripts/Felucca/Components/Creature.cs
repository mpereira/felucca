using System;
using UnityEngine;
using System.Linq;
using UnityEngine.UIElements;
using Image = UnityEngine.UI.Image;

namespace Felucca.Components {
    public class Creature : MonoBehaviour {
        public int strength;
        public int dexterity;
        public int intelligence;
        
        public int hitPoints;
        public int stamina;
        public int mana;
        
        public int movementSpeed;
        public int rotationSpeed;
        public int attackSpeed;
        
        public int attackRange;
        
        public Vector3 destination;
        
        public Creature attackee;
        public Creature[] threateners;
        public float? lastHitAttemptedAt;

        public CharacterController characterController;
        private Camera _camera;
        
        public Canvas canvas;
        public GameObject creatureBarPanel;
        private readonly RaycastHit[] _hitResults = new RaycastHit[10];
        private Vector3? _startedDraggingFrom;
        private float _dragStartThreshold = 5f;
        
        ////////////////////////////////////////////////////////////////////////
        // Lifecycle ///////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        private void Start() {
            strength = 50;
            rotationSpeed = 50;
            movementSpeed = 50;
            hitPoints = strength;
            destination = transform.localPosition;
            characterController = GetComponent<CharacterController>();
            _camera = Camera.main;

            canvas = FindObjectOfType<Canvas>();
            
            creatureBarPanel = new GameObject();
            creatureBarPanel.SetActive(false);
            creatureBarPanel.AddComponent<RectTransform>();
            creatureBarPanel.AddComponent<CanvasRenderer>();
            creatureBarPanel.AddComponent<Image>();
            creatureBarPanel.AddComponent<CreatureBar>();
            creatureBarPanel.transform.SetParent(FindObjectOfType<Canvas>().gameObject.transform);
        }

        private void OnMouseDown() {
            Debug.Log("mousedown: " + gameObject.name);
            
            if (Input.GetKeyDown(KeyCode.Mouse0)) {
                _startedDraggingFrom = Input.mousePosition;
            }
        }

        private void OnMouseDrag() {
            if (_startedDraggingFrom == null) {
                return;
            }

            var dragDistance = Vector3.Distance(
                _startedDraggingFrom.Value,
                Input.mousePosition
            );
            
            if (dragDistance < _dragStartThreshold) {
                return;
            }
            if (!creatureBarPanel.activeSelf) {
                creatureBarPanel.SetActive(true);
            }

            var targetHit = TargetHitFinder.TargetHit();
            if (targetHit == null) {
                return;
            }
            var screenPoint = _camera.WorldToScreenPoint(
                targetHit.Value.point
            );

            var position = new Vector3(
                screenPoint.x - Screen.width / 2f,
                screenPoint.y - Screen.height / 2f,
                0
            );
            creatureBarPanel.transform.localPosition = position;
        }

        private void OnMouseUp() {
            if (Input.GetKeyDown(KeyCode.Mouse0)) {
                _startedDraggingFrom = null;
            }
        }

        private void Update() {
            if (IsAlive()) {
                if (!IsAtDestination()) {
                    LookTowardsPosition(destination);
                    MoveTowardsPosition(destination);
                }
                if (attackee != null && attackee.IsAlive()) {
                    SetDestination(attackee.transform.localPosition);
                    if (WithinAttackRange(attackee)) {
                        AttemptHit(attackee);
                    }
                }
            }
        }
        
        ////////////////////////////////////////////////////////////////////////
        // Actions /////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        public void LookTowardsPosition(Vector3 position) {
            var currentPosition = transform.localPosition;
            var currentRotation = transform.localRotation;
            var targetRotation = Quaternion.LookRotation(
                Vector3.Scale(
                    new Vector3(1, 0, 1),
                    position - currentPosition
                )
            );
            transform.localRotation = Quaternion.RotateTowards(
                currentRotation,
                targetRotation,
                NormalizedRotationSpeed() * Time.deltaTime
            );
        }

        public void MoveTowardsPosition(Vector3 position) {
            if ((transform.localPosition - position).magnitude > 0.5) {
                characterController.SimpleMove(
                    transform.forward * NormalizedMovementSpeed()
                );
            }
        }

        public void StartAttacking(Creature anotherCreature) {
            attackee = anotherCreature;
        }

        public void StopAttacking() {
            attackee = null;
        }

        public void AcknowledgeAttacker(Creature attackingCreature) {
            StartAttacking(attackingCreature);
            threateners.Append<Creature>(attackingCreature);
        }

        public void Die() {
        }

        public void ReceiveHit(int damage) {
            hitPoints = Mathf.Clamp(hitPoints - damage, 0, MaxHitPoints());
            if (IsDead()) {
                Die();
            }
        }

        public void Hit(Creature anotherCreature) {
            lastHitAttemptedAt = Time.time;
            anotherCreature.ReceiveHit(5);
        }
        
        public void AttemptHit(Creature anotherCreature) {
            if (IsRecoveredFromPreviousHit()) {
                Hit(anotherCreature);
            }
        }

        public void SetDestination(Vector3 _destination) {
            destination = _destination;
        }
        
        ////////////////////////////////////////////////////////////////////////
        // Domain logic ////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        public bool WithinAttackRange(Creature anotherCreature) {
            return NormalizedAttackRange() > Vector3.Distance(
                transform.localPosition,
                anotherCreature.gameObject.transform.localPosition
            );
        }

        public bool IsRecoveredFromPreviousHit() {
            return lastHitAttemptedAt == null ||
                   Time.time - lastHitAttemptedAt > NormalizedAttackSpeed();
        }

        public int MaxHitPoints() {
            return strength;
        }

        public bool IsAlive() {
            return hitPoints > 0;
        }

        public bool IsDead() {
            return !IsAlive();
        }

        public bool IsAtDestination() {
            return destination == transform.position;
        }

        private float NormalizedRotationSpeed() {
            return rotationSpeed * 10f;
        }
        
        private float NormalizedMovementSpeed() {
            return movementSpeed / 10f;
        }
        
        private float NormalizedAttackSpeed() {
            return attackSpeed / 10f;
        }
        
        private float NormalizedAttackRange() {
            return attackRange / 50f;
        }
    }
}
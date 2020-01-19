using System;
using UnityEngine;
using System.Linq;
using JetBrains.Annotations;
using Random = UnityEngine.Random;

namespace Felucca.Components {
    public class Creature : MonoBehaviour {
        // Stats.
        public int strength;
        public int dexterity;
        public int intelligence;

        // Attributes.
        public int hitPoints;
        public int stamina;
        public int mana;

        // Skills.
        public double wrestling;

        public int movementSpeed;
        public int rotationSpeed;
        public int attackSpeed;

        public int attackRange;

        public Vector3 destination;

        [CanBeNull] public Creature   attackee;
        public             Creature[] threateners;
        public             float?     lastHitAttemptedAt;

        public CharacterController characterController;

        public float moveThreshold         = 0.5f;
        public int   resurrectionHitPoints = 1;

        public event Action<Creature, Creature>      OnHitAttempted;
        public event Action<Creature, Creature, int> OnHit;
        public event Action<Creature, Creature>      OnMiss;
        public event Action                          OnDeath;
        public event Action                          OnResurrection;

        ////////////////////////////////////////////////////////////////////////
        // Lifecycle ///////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        private void Start() {
            hitPoints = strength;
            stamina = dexterity;
            mana = intelligence;

            destination = transform.localPosition;

            characterController = GetComponent<CharacterController>();
        }

        private void Update() {
            if (IsAlive()) {
                if (!IsAtDestination()) {
                    LookTowardsPosition(destination);
                    MoveTowardsPosition(destination);
                }

                if (attackee != null && attackee.IsAlive()) {
                    StartMovingTowards(attackee.transform.localPosition);
                    if (WithinAttackRange(attackee)) {
                        AttemptHit(attackee);
                    }
                }
            }
        }

        ////////////////////////////////////////////////////////////////////////
        // Actions /////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        public int IncreaseStat(String stat, int increase) {
            switch (stat) {
                case "strength": {
                    strength += increase;
                    return strength;
                }
                case "dexterity": {
                    dexterity += increase;
                    return dexterity;
                }
                case "intelligence": {
                    intelligence += increase;
                    return intelligence;
                }
                default: return 0;
            }
        }

        public double IncreaseSkill(String skill, double increase) {
            switch (skill) {
                case "wrestling": {
                    wrestling += increase;
                    return Math.Round(wrestling, 2);
                }
                default: return 0f;
            }
        }

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
            if (Distance(position) > moveThreshold) {
                characterController.SimpleMove(
                    transform.forward * NormalizedMovementSpeed()
                );
            }
        }

        public void StartAttacking(Creature anotherCreature) {
            if (!ReferenceEquals(anotherCreature, this)) {
                attackee = anotherCreature;
            }

            anotherCreature.OnDeath += () => { OnAttackeeDeathLastHit(this); };
        }

        public Action OnAttackeeDeath(Creature anotherCreature) {
            return () => {
                StopAttacking(anotherCreature);
                StopMoving();
            };
        }

        public void OnAttackeeDeathLastHit(Creature anotherCreature) {
        }

        public void StopAttacking() {
            if (attackee == null) {
                return;
            }

            StopAttacking(attackee);
        }

        public void StopAttacking(Creature anotherCreature) {
            anotherCreature.OnDeath -= OnAttackeeDeath(this);
            if (attackee == anotherCreature) {
                attackee = null;
            }
        }

        public void AcknowledgeAttacker(Creature attackingCreature) {
            StartAttacking(attackingCreature);
            threateners.Append<Creature>(attackingCreature);
        }

        public void Die() {
            StopAttacking();
            StopMoving();
            OnDeath?.Invoke();
        }

        public void ReceiveHit(Creature attacker, int damage) {
            hitPoints = Mathf.Clamp(hitPoints - damage, 0, MaxHitPoints());
            if (IsDead()) {
                Die();
                attacker.OnAttackeeDeathLastHit(this);
            }
        }

        public void Resurrect() {
            if (IsAlive()) {
                return;
            }

            hitPoints = Mathf.Clamp(resurrectionHitPoints, 0, MaxHitPoints());
            OnResurrection?.Invoke();
        }

        public void Heal(int amount) {
            if (IsDead()) {
                return;
            }

            hitPoints = Mathf.Clamp(hitPoints + amount, 0, MaxHitPoints());
        }

        // http://www.uoguide.com/Damage_Increase
        // https://uo.stratics.com/content/arms-armor/damage.php
        public int Damage() {
            // TODO: make it depend on stats, skills, equips, etc.
            return 5 + Random.Range(0, 10);
        }

        public void Hit(Creature anotherCreature) {
            if (anotherCreature.IsDead()) {
                return;
            }

            var damage = Damage();
            anotherCreature.ReceiveHit(this, damage);
            OnHit?.Invoke(this, anotherCreature, damage);
        }

        public void Miss(Creature anotherCreature) {
            OnMiss?.Invoke(this, anotherCreature);
        }

        public int AttackSpeed() {
            return Mathf.Clamp(attackSpeed + Random.Range(0, 10), 0, 100);
        }

        public float HitChance(Creature anotherCreature) {
            // TODO: make this depend on stats, skills, the other creature etc.
            var minimumHitChance = 0.05f;
            var hitChance =
                minimumHitChance +
                dexterity / anotherCreature.dexterity;
            return Mathf.Clamp(hitChance, 0f, 1f);
        }

        public void AttemptHit(Creature anotherCreature) {
            if (IsRecoveredFromPreviousHit()) {
                lastHitAttemptedAt = Time.time;
                OnHitAttempted?.Invoke(this, anotherCreature);
                if (HitChance(anotherCreature) > Random.value) {
                    Hit(anotherCreature);
                } else {
                    Miss(anotherCreature);
                }
            }
        }

        public void StartMovingTowards(Vector3 destination) {
            this.destination = destination;
        }

        public void StopMoving() {
            destination = transform.localPosition;
        }

        ////////////////////////////////////////////////////////////////////////
        // Domain logic ////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        public float Distance(Vector3 position) {
            return Vector3.Distance(transform.localPosition, position);
        }

        public float Distance(Creature anotherCreature) {
            return Distance(anotherCreature.gameObject.transform.localPosition);
        }

        public bool WithinAttackRange(Creature anotherCreature) {
            return NormalizedAttackRange() > Distance(anotherCreature);
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
            return AttackSpeed() / 10f;
        }

        private float NormalizedAttackRange() {
            return attackRange / 20f;
        }
    }
}

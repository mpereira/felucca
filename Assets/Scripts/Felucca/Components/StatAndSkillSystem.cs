using System;
using UnityEngine;
using Random = UnityEngine.Random;

namespace Felucca.Components {
    public class StatAndSkillSystem : MonoBehaviour {
        public Creature creature;

        public event Action<String, int, int>       OnStatChange;
        public event Action<String, double, double> OnSkillChange;

        private void Start() {
            creature.OnHitAttempted += MaybeIncreaseStat("strength");
            creature.OnHitAttempted += MaybeIncreaseStat("dexterity");
            creature.OnHitAttempted += MaybeIncreaseSkill("wrestling");
        }

        private float StatIncreaseChance(
            Creature attacker, Creature attackee, String stat
        ) {
            // TODO: Make this depend on stat, etc.
            return 0.2f;
        }

        private float SkillIncreaseChance(
            Creature attacker, Creature attackee, String skill
        ) {
            // TODO: Make this depend on skill, etc..
            return 0.2f;
        }

        private Action<Creature, Creature> MaybeIncreaseStat(String stat) {
            return (attacker, attackee) => {
                if (
                    StatIncreaseChance(attacker, attackee, stat) >
                    Random.value
                ) {
                    var increase = 1;
                    var value = creature.IncreaseStat(stat, increase);
                    OnStatChange?.Invoke(stat, value, increase);
                }
            };
        }

        private Action<Creature, Creature> MaybeIncreaseSkill(String skill) {
            return (attacker, attackee) => {
                if (
                    SkillIncreaseChance(attacker, attackee, skill) >
                    Random.value
                ) {
                    var increase = 0.1;
                    var value = creature.IncreaseSkill(skill, increase);
                    OnSkillChange?.Invoke(skill, value, increase);
                }
            };
        }
    }
}

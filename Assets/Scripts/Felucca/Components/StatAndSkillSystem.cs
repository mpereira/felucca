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

        private float StatIncreaseChance(String stat) {
            // TODO: Make this depend on stat, etc.
            return 0.2f;
        }

        private float SkillIncreaseChance(String skill) {
            // TODO: Make this depend on skill, etc..
            return 0.2f;
        }

        private Action MaybeIncreaseStat(String stat) {
            return () => {
                if (StatIncreaseChance(stat) > Random.value) {
                    var increase = 1;
                    var value = creature.IncreaseStat(stat, increase);
                    OnStatChange?.Invoke(stat, value, increase);
                }
            };
        }

        private Action MaybeIncreaseSkill(String skill) {
            return () => {
                if (SkillIncreaseChance(skill) > Random.value) {
                    var increase = 0.1;
                    var value = creature.IncreaseSkill(skill, increase);
                    OnSkillChange?.Invoke(skill, value, increase);
                }
            };
        }
    }
}

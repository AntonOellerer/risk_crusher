# Leeroy Jenkins

In this section, the more in-depth aspects of Leeroy Jenkins will be explained.

## Road to the final Agent

We started out by developing the simple Leeroy agent, using heuristics for most of the tasks, to get used to the problem
and the rules of the game. Only for the initial placement we decided to use a Monte Carlo Tree search, since we
determined that this phase is separated from the rest of the game, and can thus be tackled with MCTS easier than the
other problems. By doing this, we were also able to experiment with MCTS in general, and build a first structure for
further employing it later on. Another intention of the first Leeroy agent was to have a performant, fast baseline to
evaluate future agent versions against.

After we finished and handed in the first agent, we moved on to incorporate Monte Carlo Tree Search to support Leeroy in
the other phases of the game. For this, we iterated on our experiences, and the structure developed during
implementation of the initial placement search, to build up a package and class layout which would allow us to simply
create new agents by extending the original Leeroy and plugging in different heuristics as required. Using this
structure, we first implemented the attack action selection, and the occupation action selection by MCT search.

Running evaluations on the new agent, we noticed that we were able to win against all the default agents, but not our
original one. By examining the results with our log processing tool, we noticed that the main reason the MCTS Leeroy
lost against the original one seemed to be that the base reinforcement heuristic did not perform well when used together
with the heuristics for attack and occupy for the MCTS.

Moving on, we decided to finalize our agent by incorporating the reinforcement phase into the Monte Carlo Tree search,
and by further extending it by persisting the tree between the searches of one turn of the player.

When we employed the final raw verion of the cached MCTS agent, we ran into a lot of memory issues. To solve those, we
made use of the memory profiler supplied by the IntelliJ idea to find problematic parts in our code and fix them. The
main issue was that we called the very expensive `RiskGame.getBoard()` function whenever needed, so we refactored our
code to only use one single instance of the board during each search.

Once we did that, we could notice that our agent now was able to win a majority of times against the original agent, so
we cleaned up our code, documented it, ran further evaluations, and fixed bugs as they came up.

## Core Heuristics and Algorithms

There are a few heuristics and algorithms central to Leeroy Jenkins which will be explained in this section.

### Initial Select

Initial select was the first part of the agent we implemented with Monte Carlo Tree search. The search tree is saved in
between turns, so during the whole initial placement phase Leeroy can always expand on earlier made findings. The search
is a simple search over the full search space, in the evaluation function the score of the leaf node is calculated by
incorporating the following factors:

* How many are our troops distributed over? (The fewer, the better)
* How many enemy territories are bordering directly to our territories? (The fewer, the better)
* How many continental bonus troops do we get? (The more, the better)
* How many continental bonus troops do our enemies get? (The fewer, the better)

### Reinforcement Heuristic

The reinforcement heuristic first looks for the continent, where the proportion of Leeroy's troop to the enemy's troops
is closest to 0.9, and then looks for a territory in this continent where the proportion of Leeroy's troops to
neighbouring enemy troops is close to 0.8. On this territory, it places as many troops as possible. Those thresholds
were chosen for three reasons:

* The reinforcement troops should be deployed in battles with strategic value.
* The reinforcement troops should not be deployed in battles where Leeroy has no chance of winning
* The reinforcement troops should not be deployed in battles which we would win without them alread

### Attack Heuristic

The attack action heuristic made use of insights won during our preceding literature survey, where we learned that
usually an all-out attack approach was the most beneficial strategy and tactic for winning risk games. (The general idea
is that the potential rewards are usually higher than the risks, which still have to be turned into gains by the enemy
if they happen.
[@osborne2003markov])
For this reason the simple attack heuristic looks for the battle it has the highest probability of winning by employing
a module for calculating the win probability of each fight (also taken from [@osborne2003markov]), and then does an
all-out attack as long as the probability of winning the fight is above 0.5. When the battle is over, it further looks
whether there are any battles it can win with a probability higher than 0.5.

### Occupy Heuristic

The heuristic for the occupy actions (moving troops after a territory has been conquered)
tries to maximize the number of troops in the territory which is either more in danger, of if none is in danger, the one
close to the frontlines. It first checks whether only one of the territories has an enemy territory as neighbor, if yes
it maximizes the number of troops located there. If both of them are threatened, it reinforces the territory with the
lower ratio of own troops to enemy troops. If none of the territories is adjacent to an enemy territory, it moves the
troops to the territory closer to the frontline. Lastly, if both territories are equally close to the frontline, it
leaves the decision to the calling function by returning a triple of maximizing the troops in one territory, minimizing
them, or equally balancing them.

### Fortification Heuristic

For determining fortification actions, we look for all possible fortification actions moving troops to the frontline
recursively, and then selecting the one moving the highest amount.

### MCTS eval

For evaluating the Monte Carlo Tree search leaf nodes, we used a sofisticated function involving a number of factors:

* The number of territories occupied by Leeroy. (The more, the better)
* How many enemy troops were adjacent to territories occupied by Leeroy. (The fewer, the better)
* The relation of Leeroy's frontline troops to enemy frontline troops. (The more, the better)
* The number of continental bonus troops Leeroy receives. (The more, the better)
* The number of continental bonus troops the enemy receives? (The fewer, the better)
* The number of Leeroy's troops which can not be put into battles. (The fewer, the better)

## Suppliers

To be able to simply share the heuristics between the different agents, and to have a central point for tuning them, we
relied on a `Supplier` pattern, which is able to generate desired actions for each phase. They played an important role
in later stages of our project, when we had to tune the branching factor of each step to stay in memory bounds.
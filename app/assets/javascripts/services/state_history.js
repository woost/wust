app.service("StateHistory", function(DiscourseNode, Utils) {
    var historyStates = [DiscourseNode.goal.state, DiscourseNode.problem.state, DiscourseNode.idea.state];
    var states = [];

    this.states = states;
    this.onStateChange = onStateChange;

    function onStateChange(event, toState, toParams, fromState, fromParams) {
        if (historyStates.indexOf(toState.name) < 0)
            return;

        var obj = {
            info: toState,
            params: toParams,
            css: DiscourseNode.getByState(toState.name).css
        };

        Utils.removeElementBy(states, function(state) {
            return state.info === toState && state.params.id === toParams.id;
        });

        states.push(obj);
    }
});

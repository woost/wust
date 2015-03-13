app.service('DiscourseNode', function() {
    this.goal = {
        css: "discourse_goal",
        state: "goals",
        color: "#6DFFB4",
        label: "GOAL"
    };

    this.problem = {
        css: "discourse_problem",
        state: "problems",
        color: "#FFDC6D",
        label: "PROBLEM"
    };

    this.idea = {
        css: "discourse_idea",
        state: "ideas",
        color: "#6DB5FF",
        label: "IDEA"
    };

    this.get = get;

    var mappings = _([this.goal, this.problem, this.idea]).map(function(node) {
        var res = {};
        res[node.label] = node;
        return res;
    }).reduce(_.merge);

    function get(label) {
        return mappings[label];
    }
});

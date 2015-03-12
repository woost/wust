app.service('DiscourseNode', function() {
    this.goal = {
        css: "discourse_goal",
        state: "goals",
        color: "#6DFFB4"
    };

    this.problem = {
        css: "discourse_problem",
        state: "problems",
        color: "#FFDC6D"
    };

    this.idea = {
        css: "discourse_idea",
        state: "ideas",
        color: "#6DB5FF",
    };

    this.get = get;

    var mappings = {
        GOAL: this.goal,
        PROBLEM: this.problem,
        IDEA: this.idea,
    };

    function get(label) {
        return mappings[label];
    }
});

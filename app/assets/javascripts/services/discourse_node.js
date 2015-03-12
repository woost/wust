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

    var mappings = {};
    mappings[this.goal.label] = this.goal;
    mappings[this.problem.label] = this.problem;
    mappings[this.idea.label] = this.idea;

    function get(label) {
        return mappings[label];
    }
});

angular.module("wust.elements").directive("survey", survey);

survey.$inject = [];

angular.module("wust.elements").service("SurveyService", SurveyService);

SurveyService.$inject = ["store"];

function SurveyService(store) {
    let surveyStore = store.getNamespacedStore("survey");

    this.currentIndex = surveyStore.get("currentIndex") || 0;
    // this.isFinished = surveyStore.get("isFinished") || false;

    this.finished = finished;
    this.openQuestionaire = openQuestionaire;
    this.back = back;
    this.next = next;

    function finished() {
        // this.isFinished = true;
        // surveyStore.set("isFinished", this.isFinished);
    }

    function openQuestionaire() {
        ga("send", "event", "Questionaire", "opened");
    }

    function back() {
        if (this.currentIndex < 1)
            return;

        this.currentIndex--;
        surveyStore.set("currentIndex", this.currentIndex);
    }

    function next() {
        if (this.currentIndex >= this.exercises.length - 1)
            return;


        this.currentIndex++;
        surveyStore.set("currentIndex", this.currentIndex);

        // Google Analytics Events
        if (this.currentIndex === 1)
            ga("send", "event", "Tutorial", "started");
        if (this.currentIndex === this.exercises.length - 1)
            ga("send", "event", "Tutorial", "finished");
    }

    this.exercises = [{
        titleGerman: "Willkommen bei Wust",
        title: "Welcome to Wust",
        secondTitleGerman: "Fahren Sie mit der Maus über dieses Feld",
        secondTitle: "Hover here with your mouse",
        email: true,
        descriptionGerman: `
**Wust** ist ein Graph-basiertes Diskussionsystem.

- Bitte führen Sie alle hier gestellten Aufgaben aus, um mit dem System vertraut zu werden.
- Sie entscheiden selbst, wann eine Aufgabe erledigt ist.
- Am Ende der Aufgaben finden Sie einen Link zu einem Fragebogen. Wir würden uns freuen, wenn sie diesen nach den Aufgaben ausfüllen würden.
- Die erste Aufgabe sehen Sie mit einem Klick auf **Weiter**.
`,
        description: `
**Wust** is a graph based discussion system.

- Please do all exercises to get comfortable with the system.
- You decide when an exercise is done.
- You see the first exercise with a click on **Next**.

Improvements
`
    }, {
        titleGerman: "Registrieren Sie sich",
        title: "Create an account",
        descriptionGerman: `
1. Klicken Sie rechts oben auf das Menü **Register / Login**.
1. Registrieren Sie einen Benutzer-Account.

Wenn Sie bereits einen Account haben, können Sie sich auch damit einloggen.
`,
        description: `
1. Click the menu **Register / Login** at the top right corner of the screen.
1. Create an account.

If you already have an account you can just login.
`
    }, {
        titleGerman: "Betrachten Sie einen Beitrag",
        title: "View a post",
        descriptionGerman: `
1. Klicken Sie in der Navigationsleiste auf **Dashboard**. Hier sehen Sie neue Beiträge - dargestellt als farbige Boxen.
1. Klicken Sie auf einen beliebigen Beitrag, um ihn in der Fokus-Ansicht zu lesen.
`,
        description: `
1. Click on **Dashboard** in the menu bar. Here you can see the latest posts - shown as colored boxes.
1. Click on an arbitrary post to show it in the Focus-View.
`
    }, {
        titleGerman: "Stellen Sie eine Frage zum Beitrag",
        title: "Stellen Sie eine Frage zum Beitrag",
        descriptionGerman: `
1. Klicken Sie auf **Respond**, um eine Frage zu diesem Beitrag zu stellen.
1. Wenn Ihnen keine passende Frage einfällt, stellen Sie eine beliebige Frage.
1. Im zweiten Eingabefeld können Sie auswählen, wie sich Ihr Beitrag auf den anderen bezieht (Runde Boxen). Wählen Sie bitte **Question** aus.
1. Bestätigen Sie rechts unten mit **Respond**.

In der Spalte **rechts** neben dem Beitrag sehen Sie Beiträge, die sich auf diesen beziehen.
`,
        description: `
1. Click on **Respond**, to ask a question about the post.
1. If you cannot find a suitable question, ask an arbitrary question.
1. In the second text-field you can choose, how your post relates to the other (round boxes). Choose **Question**.
1. Confirm with **Respond** at the bottom right.

In the column on the **right** side of the post you see posts which refer to it.
`
    }, {
        titleGerman: "Navigieren Sie zu Ihrer Frage und zurück",
        title: "Navigate to your question and back",
        descriptionGerman: `
1. Klicken Sie auf Ihre soeben erstellte Frage.
1. Auf der **linken Seite** sehen Sie Beiträge, auf die sich dieser Beitrag bezieht.
1. Klicken Sie wieder auf den ursprünglichen Beitrag.
`,
        description: `
1. Click on your just created question.
1. On the **left side** you see posts, which this post refers to.
1. Click on the previous post to go back again.
`
    }, {
        titleGerman: "Wechseln Sie in die Graph-Ansicht",
        title: "Switch to the Graph-View",
        descriptionGerman: `
1. Klicken Sie nun auf der linken Seite auf das Symbol ![graph-icon](assets/images/icon-graph.png), um in die Graph-Ansicht zu wechseln.
1. Klicken Sie auf das **+** Symbol über dem ursprünglichen Beitrag und stellen Sie noch eine Frage.
1. Fügen Sie eine Idee zu Ihrer gestellten Frage hinzu.
1. Das zweite Symbol über den Beiträgen lässt Sie den Beitrag durch Ziehen mit anderen verbinden. Verbinden Sie die Idee mit der anderen Frage und klassifizieren Sie die Verbindung als Idee.
1. Experimentieren Sie ein bisschen. Verbinden Sie beispielsweise einen beliebigen Beitrag mit einer Verbindung.
`,
        description: `
1. On the left side, click on the symbol ![graph-icon](assets/images/icon-graph.png), to switch to the Graph-View.
1. Click on the **+** symbol above the current post and ask another question.
1. Add an Idea to your just asked question.
1. The second symbol above the posts lets you connect the post with others. Connect the idea with another question and classify the connection as an idea.
1. Experiment a bit. For example connect a post with a connection.
`
    }, {
        titleGerman: "Starten Sie eine neue Diskussion",
        title: "Start a discussion",
        descriptionGerman: `
1. Starten Sie eine neue Diskussion indem Sie oben in der Navigationsleiste auf den Knopf **Start Discussion** klicken. Wenn Ihnen nichts einfällt: Nehmen Sie an, Sie hätten ein Problem in Ihrem Haushalt und möchten andere Leute um Hilfe bitten.
1. Wählen Sie im zweiten Eingabefeld einen passenden Kontext (eckige Boxen) aus, z.B. Haushalt. Um einen neuen Kontext zu erzeugen, geben Sie den Namen ein und bestätigen Sie mit ENTER.
1. Veröffentlichen Sie die neue Diskussion mit einem Klick auf **Start Discussion**.

Es gibt 2 unterschiedliche **Tag**-Arten: **Klassifikationen** (Frage, Idee, ...) und **Kontexte** (Themengebiete wie Haushalt, Politik). Klassifikationen gelten immer im Bezug auf einen anderen Beitrag oder einen Kontext. **Neue Diskussionen müssen mindestens einen Kontext haben**. Definieren Sie einen oder auch mehrere Kontexte, damit Ihr Beitrag besser gefunden werden kann.
`,
        description: `
1. Start a new discussion by clicking **Start Discussion** in the menu bar. If nothing comes to mind: Suppose you have a problem in your household and want to ask other people for help.
1. In the second input-field choose a suitable context (rectangular boxes), for example Household. To create a context, enter the name and press ENTER.
1. Publish the new discussion by clicking on **Start Discussion**.

There are 2 different kinds of **Tags**: **Classifications** (Question, Idea, ...) and **Contexts** (Topics like Household, Politics).
Classifications always apply in a relation to another post or a context.
**New Discussions need to have at least one Context**. Add one or more contexts, so that your post can be discovered more efficiently.
`
    }, {
        titleGerman: "Bearbeiten Sie einen fremden Beitrag",
        title: "Edit a post from someone else",
        descriptionGerman: `
1. Gehen Sie zum **Dashboard** und sehen Sie sich einen weiteren fremden Beitrag in der Fokus-Ansicht an.
1. Klicken Sie rechts oben auf den Knopf mit dem Stift-Symbol, um den Beitrag zu bearbeiten.
1. Ändern Sie etwas in der Beschreibung des Beitrags.
1. Beachten Sie den Fortschrittsbalken über dem **Save**-Knopf.

Wenn Sie etwas an einem fremden Beitrag verändern, müssen andere Nutzer der Änderung zustimmen. Die Anzahl der nötigen Stimmen hängt davon ab, wie oft der Beitrag angesehen wurde.

Für akzeptierte Änderungen erhalten sie **Karma**-Punkte in dem jeweiligen Kontext (Themenbereich). Damit haben Sie mehr Einfluss in der Abstimmung über Änderungen in diesem Kontext.
`,
        description: `
1. Go to **Dashboard** and open a post from someone else in the Focus-View.
1. Click on the pencil-symbol on the top right corner to edit the post.
1. Change something in the description of the post.
1. Notice the progress bar above the **Save** button.

If you change something in a post from someone else, other users have to approve the change. The amount of necessary votes depends on how often the post has been viewed.

For approved changes you receive **Karma**-Points in the respective context (topic). With these points you have more influence on the voting on changes in this context.
`
    }, {
        titleGerman: "Bewerten Sie Änderungen anderer Nutzer",
        title: "Review changes of other users",
        descriptionGerman: `
1. Klicken Sie in der Navigationsleiste auf den Eintrag **Moderation**. Hier können Sie über vorgeschlagene Änderungen anderer Nutzer abstimmen. Sie entscheiden mit, welche Änderungen akzeptiert und welche abgelehnt werden.
1. Stimmen Sie über ein oder zwei Änderungen ab.

Wurden viele Ihrer eigenen Änderungsvorschläge akzeptiert, haben Sie mehr **Karma**-Punkte und damit mehr Stimmgewicht. In einigen Fällen führt das dazu, dass Ihre Änderungen sofort sichtbar sind - Nutzer können dann nachträglich darüber abstimmen. Wird eine Änderung abgelehnt, so wird sie automatisch rückgängig gemacht.
`,
        description: `
1. In the menu bar, click on the button **Moderation**. Here you can see changes made by other users. You can decide whether these changes should be accepted or rejected.
1. Vote on one or two changes.

If many of your own changes got accepted, you get more **Karma**-points and therefore your votes have more weight. In some cases this leads to your changes being visible immediately - users can vote on these changes retroactively. If such a change is rejected, it gets reverted automatically.
`
    }, {
        titleGerman: "Bitte füllen Sie den Fragebogen aus",
        title: "You are done!",
        email: true,
        descriptionGerman: `
Vielen Dank, Sie haben alle Aufgaben erledigt.

Schauen Sie sich nun weiter im System um. Wenn Sie möchten, können Sie gerne weitere Diskussionen starten und sich an anderen beteiligen, bevor Sie den Fragebogen ausfüllen.
`,
        description: `
Thank you, you completed all exercises.

Now look around in the system, start more discussions or participate in existing ones.

Wust is Open Source Software and available on (GitHub)[https://github.com/woost/wust].
`
    }];
}

function survey() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "elements/survey/survey.html",
        scope: {
            canHide: "@"
        },
        controller: surveyCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

surveyCtrl.$inject = ["SurveyService"];

function surveyCtrl(SurveyService) {
    let vm = this;

    vm.survey = SurveyService;

}

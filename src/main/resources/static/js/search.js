$(document).ready(function () {
    "object" == typeof res && res && ($(".item-single").click(function (a) {
        if ($(a.target).is($(".jp-pause"))) return !1;
        var b = $(this).data("id");
        $("#player" + b).jPlayer("setMedia", {title: b, mp3: res[b]}), $("#player" + b).jPlayer("play")
    }), $(".item-single").each(function () {
        var a = $(this).data("id"), b = $("#item" + a), c = $("#player" + a);
        c.jPlayer({
            ready: function (a) {
                b.data("ready", !0)
            },
            play: function () {
                $(this).jPlayer("tellOthers", "stop")
            },
            swfPath: $.luoo.root + "/static/thirdparty/jplayer",
            volume: 1,
            globalVolume: !0,
            cssSelectorAncestor: "#item" + a,
            wmode: "window"
        })
    }), $(".play-btn-mask").click(function (a) {
        $(this).siblings(".play-btn:visible").click()
    }))
});
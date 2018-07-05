function init_avatarUploader() {
    $.luoo.msg("修改头像", '<div class="avatar-outer"><div id="avatar-upload"></div></div>', !1, "#000", !1);
    var a = $("#avatarEditWrapper").find(".avatar"), b = "11.1.0", c = "playerProductInstall.swf", d = {
        url: "http://upload.qiniu.com",
        token: uploadToken,
        key: key,
        name: "file",
        width: 700,
        height: 700,
        pic: a.attr("src")
    }, e = {};
    e.quality = "high", e.bgcolor = "#ffffff", e.allowscriptaccess = CONST.host, e.allowfullscreen = "false";
    var f = {};
    f.id = "avatar-upload", f.name = "avatar_upload", f.align = "middle", swfobject.embedSWF(CONST.host + "/static/img/avatar.swf", "avatar-upload", "400", "500", b, c, d, e, f), swfobject.createCSS("#avatar-upload", "display:block;text-align:left;")
}

function unlike_cback(a, b) {
    b.data("disabled", !1);
    var c = b.parents(".track-item");
    if (-1 === a.status) {
        b.data({remote: url("login/dialog"), width: 235});
        var d = b.tip();
        d.set("events.hide", function (a, b) {
            b.destroy(), $.luoo.destroy_tip(b.id)
        }), $.luoo.tips.push(d)
    } else if (1 === a.status) c.find(".btn-action-like").addClass("btn-faved").attr("title", b.data("langliked")); else if (2 === a.status) {
        var e = c.index();
        "play" == c.data("status") && luooPlayer.next(), luooPlayer.remove(e) && c.fadeOut(500, function () {
            $(this).remove()
        })
    } else a.msg && $.luoo.tip(a.msg)
}

function vol_unlike_cback(a, b) {
    if (b.data("disabled", !1), -1 === a.status) {
        b.data({remote: url("login/dialog"), width: 235});
        var c = b.tip();
        c.set("events.hide", function (a, b) {
            b.destroy(), $.luoo.destroy_tip(b.id)
        }), $.luoo.tips.push(c)
    } else 2 === a.status ? document.location.reload() : a.msg && $.luoo.tip(a.msg)
}

function init_uploader() {
    $("#btnAvatarUploader").fineUploader({
        autoUpload: !0,
        validation: {allowedExtensions: ["jpeg", "jpg", "png"], sizeLimit: 2097152},
        request: {endpoint: url("user/upload"), inputName: "luooUpload"}
    }).on("upload", function (a, b) {
        $("#btnAvatarUploader").hide(), $("#btnAvatarUploading").show()
    }).on("complete", function (a, b, c, d) {
        if (d.success) {
            $("#resizePic").data("oldWidth", d.width).data("oldHeight", d.height), $("#txtAvatar").val(d.url), $("#resizePic").attr("src", d.url), $("#previewPic").attr("src", d.url), $("#uploadHolder").hide(), $("#resizeHolder").show(), picEditor = $("#resizePic").imgAreaSelect({
                handles: !0,
                aspectRatio: "1:1",
                instance: !0,
                imageHeight: d.height,
                imageWidth: d.width,
                classPrefix: "imgareaselect",
                onSelectEnd: function (a, b) {
                    change_preview(b)
                }
            });
            var e, f;
            e = f = d.width > d.height ? d.height - 1 : d.width - 1, $("#resizePic").load(function () {
                picEditor.setOptions({
                    x1: 0,
                    y1: 0,
                    x2: e,
                    y2: f,
                    show: !0,
                    enable: !0,
                    disable: !1
                }), picEditor.update(), change_preview(picEditor.getSelection())
            })
        } else $("#btnAvatarUploader").show(), $("#btnAvatarUploading").hide(), d.error && $("#uploadMsg").html('<span style="color: #E43E4A;">' + d.error + "</span>")
    });
    $("#btnCancleAvatar").click(function () {
        reset_avatar()
    }), $("#btnSubmitAvatar").click(function () {
        $("#frmAvatar").submit()
    }), $("#lnEditMood").click(function () {
        $("#textMood").hide(), $("#formMood").show()
    }), $("#txtMoodCt").dblclick(function () {
        $(this).data("editable") && ($("#textMood").hide(), $("#formMood").show())
    }), $("#lnCancleMood").click(function () {
        $("#formMood").hide(), $("#textMood").show()
    }), $("#lnSaveMood").click(function () {
        var a = $("#txtContent").val();
        return a.length > 140 && $.luoo.msg("个性签名请控制在140字以内"), a == $("#txtContent").data("value") ? ($("#formMood").hide(), $("#textMood").show(), !1) : void $(this).parents("form").submit()
    }), $("#txtContent").keyup(function (a) {
        27 == a.keyCode ? ($("#formMood").hide(), $("#textMood").show()) : 13 == a.keyCode && $("#lnSaveMood").click()
    })
}

function change_preview(a) {
    if (0 == a.width || 0 == a.height) return !1;
    var b = 50 / a.width, c = 50 / a.height;
    $("#previewPic").css({
        width: Math.round(b * $("#resizePic").data("oldWidth")) + "px",
        height: Math.round(c * $("#resizePic").data("oldHeight")) + "px",
        marginLeft: "-" + Math.round(b * a.x1) + "px",
        marginTop: "-" + Math.round(c * a.y1) + "px"
    }), $("#x").val(a.x1), $("#y").val(a.y1), $("#w").val(a.width), $("#h").val(a.height)
}

function reset_avatar() {
    $.luoo.close_msg(), "undefined" != typeof picEditor && picEditor.cancelSelection()
}

function avatar_cback(a, b) {
    return a.status ? ("undefined" != typeof picEditor && picEditor.cancelSelection(), $("#resizeHolder").animate({height: 0}, 300), $("#msgHolder").html("操作成功，窗口将在3秒后关闭。").show(), setTimeout(function () {
        document.location.reload()
    }, 3e3)) : ("undefined" != typeof picEditor && picEditor.cancelSelection(), $("#uploadMsg").html('<span style="color: #E43E4A;">' + a.msg + "</span>"), $("#resizeHolder").hide(), $("#uploadHolder").show()), !1
}

function add_mood_cback(a, b) {
    if (b.data("disabled", !1), a.status) {
        var c = a.msg ? a.msg : "说两句吧...";
        $("#txtMoodCt").html(c), $("#formMood").hide(), $("#textMood").show(), $("#txtContent").data("value", a.msg)
    } else $.luoo.msg("操作失败", a.msg)
}

var luooPlayer, log_int, picEditor;
$(document).ready(function () {
    init_player({autoplay: !1}), $(".track-wrapper").hover(function () {
        $(this).find(".btn-detail, .btn-unfav, .btn-share, .btn-fav").css("visibility", "visible")
    }, function () {
        return $(this).siblings(".track-detail-wrapper").is($(":visible")) || "play" == $(this).parents(".track-item").data("status") ? !1 : ($(this).find(".btn-detail, .btn-unfav, .btn-share, .btn-fav").css("visibility", "hidden"), $(".btn-unfaved").css("visibility", "visible"), void $(".btn-faved").css("visibility", "visible"))
    }), $("#lnEditAvatar").click(function () {
        init_avatarUploader()
    }), $(".medal").click(function () {
        var a = $(this).data();
        $.luoo.msg("落网徽章", '<div class="medal-outer"><img src="' + a.src + '" width="100" class="medal-img"> <p class="medal-desc">' + a.desc + "</p></div>", !1, "#000", !1)
    }), $("#avatarEditWrapper").hover(function () {
        $("#lnEditAvatar").show()
    }, function () {
        $("#lnEditAvatar").hide()
    })
});
var ASFace = {
    callback: function (a) {
        var b = $.parseJSON(a), c = $("#avatarEditWrapper").find(".avatar"), d = new Date - 0;
        c.attr("src", CONST.cloud_img + "/" + b.key + "?imageView2/1/w/128/h/128&t=" + d), $.ajax({
            url: CONST.host + "/user/update_avatar",
            method: "post",
            data: {avatar: b.key},
            dataType: "json",
            success: function (a) {
                1 == a.status ? $.luoo.close_msg() : $.luoo.msg("操作失败", a.msg)
            },
            error: function () {
                $.luoo.msg("操作失败", a.msg)
            }
        })
    }, cancel: function () {
        $.luoo.close_msg()
    }
};
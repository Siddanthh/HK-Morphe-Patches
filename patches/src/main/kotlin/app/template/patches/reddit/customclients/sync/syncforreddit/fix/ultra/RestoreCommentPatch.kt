package app.template.patches.reddit.customclients.sync.syncforreddit.fix.ultra

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.string.replaceStringPatch
import app.template.patches.reddit.customclients.sync.syncforreddit.SyncForRedditCompatible

val restoreCommentShortcutPatch = bytecodePatch(
    name = "Fix \"Restore Comment\"",
    description = "Fixes the \"Restore Comment\" feature (requires Sync Ultra) by fetching from an alternative API. Also adds a more accessible button for this feature."
) {
    compatibleWith(*SyncForRedditCompatible)

    dependsOn(
        // patch the Restore Comment feature to fetch from Arctic Shift
        replaceStringPatch(
            "https://api.pushshift.io/reddit/comment/search/",
            "https://arctic-shift.photon-reddit.com/api/comments/ids",
            comparison = StringComparisonType.EQUALS
        )
    )

    execute {
        var commentClassType = "Lxa/d;"

        CommentHolderBindFingerprint.method.apply {
            commentClassType = this.parameters.first().type
        }

        // replace the profile button's icon with restore comment's icon
        CommentHolderBindFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual {p1}, $commentClassType->e()Ljava/lang/String;
                move-result-object v0
                
                const-string v1, "[removed]"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
                move-result v1
                if-nez v1, :is_deleted
                
                const-string v1, "[deleted]"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :not_deleted
                
                :is_deleted
                const v0, 0x7f080642
                goto :set_icon
                
                :not_deleted
                const v0, 0x7f08025e
                
                :set_icon
                iget-object v1, p0, Lcom/laurencedawson/reddit_sync/ui/viewholders/comments/CommentHolder;->mButtonProfile:Lcom/laurencedawson/reddit_sync/ui/views/buttons/ProfileButton;
                invoke-virtual {v1, v0}, Landroidx/appcompat/widget/AppCompatImageButton;->setImageResource(I)V
            """.trimIndent()
        )

        // replace the profile button's click listener with restore comment's click listener
        CommentHolderOnProfileClickedFingerprint.method.addInstructions(
            0,
            """
                instance-of v0, p0, Lcom/laurencedawson/reddit_sync/ui/viewholders/comments/CommentHolder;
                if-eqz v0, :not_comment_holder
                
                move-object v0, p0
                check-cast v0, Ltb/a;
                invoke-virtual {v0}, Ltb/a;->j()$commentClassType
                move-result-object v0
                
                invoke-virtual {v0}, $commentClassType->e()Ljava/lang/String;
                move-result-object v1
                
                const-string v0, "[removed]"
                invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
                move-result v0
                if-nez v0, :is_removed
                
                const-string v0, "[deleted]"
                invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :not_removed
                
                :is_removed
                move-object v0, p0
                check-cast v0, Ltb/a;
                invoke-virtual {v0}, Ltb/a;->j()$commentClassType
                move-result-object v0
                new-instance v1, Lr8/h;
                invoke-direct {v1, v0}, Lr8/h;-><init>($commentClassType)V
                invoke-static {v1}, Lm8/a;->a(Lcom/android/volley/Request;)V
                return-void
                
                :not_comment_holder
                :not_removed
            """.trimIndent()
        )

        // replace comment text indicating restoration failed
        RestoreCommentRequestParseFingerprint.method.apply {
            val initJsonIndex = this.implementation?.instructions?.indexOfFirst { it.toString().contains("Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V") } ?: -1
            if (initJsonIndex != -1) {
                replaceInstruction(
                    initJsonIndex,
                    """
                        const-string v5, "\"data\":[]"
                        invoke-virtual {v3, v5}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                        move-result v5
                        if-eqz v5, :not_empty
                        
                        :is_empty
                        new-instance v5, Ljava/lang/StringBuilder;
                        invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V
                        const-string v6, "{\"data\":[{\"id\":\""
                        invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                        iget-object v6, p0, Lr8/h;->a:$commentClassType
                        invoke-virtual {v6}, $commentClassType->U()Ljava/lang/String;
                        move-result-object v6
                        invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                        const-string v6, "\",\"author\":\"unknown\",\"body\":\"[failed to restore comment]\"}]}"
                        invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                        invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                        move-result-object v3
                        
                        :not_empty
                        invoke-direct {v4, v3}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V
                    """.trimIndent()
                )
            }

            val parseBodyIndex = this.implementation?.instructions?.indexOfFirst { it.toString().contains("Lwc/p;->a(Ljava/lang/String;)Ljava/lang/String;") } ?: -1
            if (parseBodyIndex != -1) {
                replaceInstruction(
                    parseBodyIndex,
                    """
                        invoke-static {v5}, Lwc/p;->a(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v5
                        
                        const-string v11, "[removed]"
                        invoke-virtual {v5, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                        move-result v11
                        if-eqz v11, :check_deleted
                        goto :replace_body
                        
                        :check_deleted
                        const-string v11, "[deleted]"
                        invoke-virtual {v5, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                        move-result v11
                        if-eqz v11, :continue
                        
                        :replace_body
                        const-string v5, "[failed to restore comment]"
                        
                        :continue
                    """.trimIndent()
                )
            }
        }
    }
}

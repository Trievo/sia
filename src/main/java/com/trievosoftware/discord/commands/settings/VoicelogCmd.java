/*
 * Copyright 2018 Mark Tripoli (mark.tripoli@trievosoftware.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.trievosoftware.discord.commands.settings;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.trievosoftware.discord.Sia;
import com.trievosoftware.discord.commands.LogCommand;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 *
 * @author Mark Tripoli (mark.tripoli@trievosoftware.com)
 */
public class VoicelogCmd extends LogCommand
{
    public VoicelogCmd(Sia sia)
    {
        super(sia);
        this.name = "voicelog";
        this.help = "sets channel to log voice changes";
    }

    @Override
    protected void showCurrentChannel(CommandEvent event)
    {
        TextChannel tc = sia.getDatabaseManagers().getGuildSettingsService().getSettings(event.getGuild()).getVoiceLogChannel(event.getGuild());
        if(tc==null)
            event.replyWarning("Voice Logs are not currently enabled on the server. Please include a channel name.");
        else
            event.replySuccess("Voice Logs are currently being sent in "+tc.getAsMention()
                    +(event.getSelfMember().hasPermission(tc, REQUIRED_PERMS) ? "" : "\n"+event.getClient().getWarning()+String.format(REQUIRED_ERROR, tc.getAsMention())));
    }

    @Override
    protected void setLogChannel(CommandEvent event, TextChannel tc)
    {
        sia.getDatabaseManagers().getGuildSettingsService().setVoiceLogChannel(event.getGuild(), tc);
        if(tc==null)
            event.replySuccess("Voice Logs will not be sent");
        else
            event.replySuccess("Voice Logs will now be sent in "+tc.getAsMention());
    }
}

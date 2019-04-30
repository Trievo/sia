/*
 * Copyright 2017 Mark Tripoli (mark.tripoli@trievosoftware.com).
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
package com.trievosoftware.discord.logging;

import com.trievosoftware.discord.Sia;
import com.trievosoftware.discord.logging.MessageCache.CachedMessage;
//import com.jagrosh.sia.pro.AvatarUtil;
import com.trievosoftware.discord.utils.FormatUtil;
import com.trievosoftware.discord.utils.LogUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.core.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 *
 * @author Mark Tripoli (mark.tripoli@trievosoftware.com)
 */
public class BasicLogger
{
    private final static String EDIT = "\u26A0"; // ⚠
    private final static String DELETE = "\u274C"; // ❌
    private final static String BULK_DELETE = "\uD83D\uDEAE"; // 🚮
    private final static String VIEW = "\uD83D\uDCC4"; // 📄
    private final static String DOWNLOAD = "\uD83D\uDCE9"; // 📩
    private final static String REDIRECT = "\uD83D\uDD00"; // 🔀
    private final static String REDIR_MID = "\uD83D\uDD39"; // 🔹
    private final static String REDIR_END = "\uD83D\uDD37"; // 🔷
    private final static String NAME = "\uD83D\uDCDB"; // 📛
    private final static String JOIN = "\uD83D\uDCE5"; // 📥
    private final static String NEW = "\uD83C\uDD95"; // 🆕
    private final static String LEAVE = "\uD83D\uDCE4"; // 📤
    private final static String AVATAR = "\uD83D\uDDBC"; // 🖼
    
    private final Sia sia;
    
    public BasicLogger(Sia sia)
    {
        this.sia = sia;
    }
    
    private void log(OffsetDateTime now, TextChannel tc, String emote, String message, MessageEmbed embed)
    {
        try
        {
            tc.sendMessage(new MessageBuilder()
                .append(FormatUtil.filterEveryone(LogUtil.basiclogFormat(now,
                    sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(tc.getGuild())
                        .getGuildSettings().getTimezone(), emote, message)))
                .setEmbed(embed)
                .build()).queue();
        }
        catch(PermissionException ignore) {}
    }
    
    private void logFile(OffsetDateTime now, TextChannel tc, String emote, String message, byte[] file, String filename)
    {
        try
        {
            tc.sendFile(file, filename, new MessageBuilder()
                .append(FormatUtil.filterEveryone(LogUtil.basiclogFormat(now,
                    sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(tc.getGuild())
                        .getGuildSettings().getTimezone(), emote, message)))
                .build()).queue();
        }
        catch(PermissionException ignore) {}
    }
    
    // Message Logs
    
    public void logMessageEdit(Message newMessage, CachedMessage oldMessage)
    {
        if(oldMessage==null)
            return;
        TextChannel mtc = oldMessage.getTextChannel(sia.getShardManager());
        PermissionOverride po = mtc.getPermissionOverride(mtc.getGuild().getSelfMember());
        if(po!=null && po.getDenied().contains(Permission.MESSAGE_HISTORY))
            return;
        TextChannel tc = sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(newMessage.getGuild())
            .getGuildSettings().getMessageLogChannel(newMessage.getGuild());
        if(tc==null)
            return;
        if(newMessage.getContentRaw().equals(oldMessage.getContentRaw()))
            return;
        EmbedBuilder edit = new EmbedBuilder()
                .setColor(Color.YELLOW)
                .appendDescription("**From:** ")
                .appendDescription(FormatUtil.formatMessage(oldMessage));
        String newm = FormatUtil.formatMessage(newMessage);
        if(edit.getDescriptionBuilder().length()+9+newm.length()>2048)
            edit.addField("To:", newm.length()>1024 ? newm.substring(0,1016)+" (...)" : newm, false);
        else
            edit.appendDescription("\n**To:** "+newm);
        log(newMessage.getEditedTime()==null ? newMessage.getCreationTime() : newMessage.getEditedTime(), tc, EDIT, 
                FormatUtil.formatFullUser(newMessage.getAuthor())+" edited a message in "+newMessage.getTextChannel().getAsMention()+":", edit.build());
    }
    
    public void logMessageDelete(CachedMessage oldMessage)
    {
        if(oldMessage==null)
            return;
        Guild guild = oldMessage.getGuild(sia.getShardManager());
        if(guild==null)
            return;
        TextChannel mtc = oldMessage.getTextChannel(sia.getShardManager());
        PermissionOverride po = mtc.getPermissionOverride(guild.getSelfMember());
        if(po!=null && po.getDenied().contains(Permission.MESSAGE_HISTORY))
            return;
        TextChannel tc = sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(guild)
            .getGuildSettings().getMessageLogChannel(guild);
        if(tc==null)
            return;
        String formatted = FormatUtil.formatMessage(oldMessage);
        if(formatted.isEmpty())
            return;
        EmbedBuilder delete = new EmbedBuilder()
                .setColor(Color.RED)
                .appendDescription(formatted);
        User author = oldMessage.getAuthor(sia.getShardManager());
        String user = author==null ? FormatUtil.formatCachedMessageFullUser(oldMessage) : FormatUtil.formatFullUser(author);
        log(OffsetDateTime.now(), tc, DELETE, user+"'s message has been deleted from "+mtc.getAsMention()+":", delete.build());
    }
    
    public void logMessageBulkDelete(List<CachedMessage> messages, int count, TextChannel text)
    {
        if(count==0)
            return;
        TextChannel tc = sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(text.getGuild())
            .getGuildSettings().getMessageLogChannel(text.getGuild());
        if(tc==null)
            return;
        if(messages.isEmpty())
        {
            log(OffsetDateTime.now(), tc, "\uD83D\uDEAE", "**"+count+"** messages were deleted from "+text.getAsMention()+" (**"+messages.size()+"** logged)", null);
            return;
        }
        TextChannel mtc = messages.get(0).getTextChannel(sia.getShardManager());
        PermissionOverride po = mtc.getPermissionOverride(mtc.getGuild().getSelfMember());
        if(po!=null && po.getDenied().contains(Permission.MESSAGE_HISTORY))
            return;
        if(messages.size()==1)
        {
            String formatted = FormatUtil.formatMessage(messages.get(0));
            if(formatted.isEmpty())
                return;
            EmbedBuilder delete = new EmbedBuilder()
                    .setColor(Color.RED)
                    .appendDescription(formatted);
            User author = messages.get(0).getAuthor(sia.getShardManager());
            String user = author==null ? FormatUtil.formatCachedMessageFullUser(messages.get(0)) : FormatUtil.formatFullUser(author);
            log(OffsetDateTime.now(), tc, DELETE, user+"'s message has been deleted from "+mtc.getAsMention()+":", delete.build());
            return;
        }
        sia.getTextUploader().upload(LogUtil.logCachedMessagesForwards("Deleted Messages", messages, sia.getShardManager()), "DeletedMessages", (view, download) ->
        {
            log(OffsetDateTime.now(), tc, BULK_DELETE, "**"+count+"** messages were deleted from "+text.getAsMention()+" (**"+messages.size()+"** logged):", 
                new EmbedBuilder().setColor(Color.RED.darker().darker())
                .appendDescription("[`"+VIEW+" View`]("+view+")  |  [`"+DOWNLOAD+" Download`]("+download+")").build());
        });
    }
    
    public void logRedirectPath(Message message, String link, List<String> redirects)
    {
        TextChannel tc = sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(message.getGuild())
            .getGuildSettings().getMessageLogChannel(message.getGuild());
        if(tc==null)
            return;
        StringBuilder sb = new StringBuilder(REDIR_END+" **"+link+"**");
        for(int i=0; i<redirects.size(); i++)
            sb.append("\n").append(redirects.size()-1==i ? REDIR_END + " **" : REDIR_MID).append(redirects.get(i)).append(redirects.size()-1==i ? "**" : "");
        log(OffsetDateTime.now(), tc, REDIRECT, 
                FormatUtil.formatFullUser(message.getAuthor())+"'s message in "+message.getTextChannel().getAsMention()+" contained redirects:", 
                new EmbedBuilder().setColor(Color.BLUE.brighter().brighter()).appendDescription(sb.toString()).build());
    }
    
    
    // Server Logs
    
    public void logNameChange(UserUpdateNameEvent event)
    {
        OffsetDateTime now = OffsetDateTime.now();
        event.getUser().getMutualGuilds().stream()
            .map(guild -> sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(guild)
                .getGuildSettings().getServerLogChannel(guild))
            .filter(tc -> tc!=null)
            .forEachOrdered(tc ->
            {
                log(now, tc, NAME, "**"+event.getOldName()+"**#"+event.getUser().getDiscriminator()+" (ID:"
                        +event.getUser().getId()+") has changed names to "+FormatUtil.formatUser(event.getUser()), null);
            });
    }
    
    public void logNameChange(UserUpdateDiscriminatorEvent event)
    {
        OffsetDateTime now = OffsetDateTime.now();
        event.getUser().getMutualGuilds().stream()
            .map(guild -> sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(guild)
                .getGuildSettings().getServerLogChannel(guild))
            .filter(tc -> tc!=null)
            .forEachOrdered(tc ->
            {
                log(now, tc, NAME, "**"+event.getUser().getName()+"**#"+event.getOldDiscriminator()+" (ID:"
                        +event.getUser().getId()+") has changed names to "+FormatUtil.formatUser(event.getUser()), null);
            });
    }
    
    public void logGuildJoin(GuildMemberJoinEvent event)
    {
        TextChannel tc = sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(event.getGuild())
            .getGuildSettings().getServerLogChannel(event.getGuild());
        if(tc==null)
            return;
        OffsetDateTime now = OffsetDateTime.now();
        long seconds = event.getUser().getCreationTime().until(now, ChronoUnit.SECONDS);
        log(now, tc, JOIN, FormatUtil.formatFullUser(event.getUser())+" joined the server. "
                +(seconds<16*60 ? NEW : "")
                +"\nCreation: "+event.getUser().getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)+" ("+FormatUtil.secondsToTimeCompact(seconds)+" ago)", null);
    }
    
    public void logGuildLeave(GuildMemberLeaveEvent event)
    {
        TextChannel tc = sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(event.getGuild())
            .getGuildSettings().getServerLogChannel(event.getGuild());
        if(tc==null)
            return;
        OffsetDateTime now = OffsetDateTime.now();
        long seconds = event.getMember().getJoinDate().until(now, ChronoUnit.SECONDS);
        StringBuilder rlist;
        if(event.getMember().getRoles().isEmpty())
            rlist = new StringBuilder();
        else
        {
            rlist= new StringBuilder("\nRoles: `"+event.getMember().getRoles().get(0).getName());
            for(int i=1; i<event.getMember().getRoles().size(); i++)
                rlist.append("`, `").append(event.getMember().getRoles().get(i).getName());
            rlist.append("`");
        }
        log(now, tc, LEAVE, FormatUtil.formatFullUser(event.getUser())+" left or was kicked from the server. "
                +"\nJoined: "+event.getMember().getJoinDate().format(DateTimeFormatter.RFC_1123_DATE_TIME)+" ("+FormatUtil.secondsToTimeCompact(seconds)+" ago)"
                +rlist.toString(), null);
    }
    
    
    // Voice Logs
    
    public void logVoiceJoin(GuildVoiceJoinEvent event)
    {
        TextChannel tc = sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(event.getGuild())
            .getGuildSettings().getVoiceLogChannel(event.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, "<:microphone:>", FormatUtil.formatFullUser(event.getMember().getUser())
                +" has joined voice channel _"+event.getChannelJoined().getName()+"_", null);
    }
    
    public void logVoiceMove(GuildVoiceMoveEvent event)
    {
        TextChannel tc = sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(event.getGuild())
            .getGuildSettings().getVoiceLogChannel(event.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, "<:microphone:>", FormatUtil.formatFullUser(event.getMember().getUser())
                +" has moved voice channels from _"+event.getChannelLeft().getName()+"_ to _"+event.getChannelJoined().getName()+"_", null);
    }
    
    public void logVoiceLeave(GuildVoiceLeaveEvent event)
    {
        TextChannel tc = sia.getServiceManagers().getDiscordGuildService().getDiscordGuild(event.getGuild())
            .getGuildSettings().getVoiceLogChannel(event.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, "<:microphone:>", FormatUtil.formatFullUser(event.getMember().getUser())
                +" has left voice channel _"+event.getChannelLeft().getName()+"_", null);
    }
    
    
    // Avatar Logs
    
//    public void logAvatarChange(UserUpdateAvatarEvent event)
//    {
//        List<TextChannel> logs = event.getUser().getMutualGuilds().stream()
//            .map(guild -> sia.getServiceManagers().getGuildSettingsService().getSettings(guild).getAvatarLogChannel(guild))
//            .filter(tc -> tc!=null)
//            .collect(Collectors.toList());
//        if(logs.isEmpty())
//            return;
//        OffsetDateTime now = OffsetDateTime.now();
//        sia.getThreadpool().execute(() ->
//        {
//            byte[] im = AvatarUtil.makeAvatarImage(event.getUser(), event.getOldAvatarUrl(), event.getOldAvatarId());
//            if(im!=null)
//                logs.forEach(tc -> logFile(now, tc, AVATAR, FormatUtil.formatFullUser(event.getUser())+" has changed avatars"
//                        +(event.getUser().getAvatarId()!=null && event.getUser().getAvatarId().startsWith("a_") ? " <:gif:314068430624129039>" : "")
//                        +":", im, "AvatarChange.png"));
//        });
//    }
}

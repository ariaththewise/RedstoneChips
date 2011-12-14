
package org.tal.redstonechips.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tal.redstonechips.RedstoneChips;
import org.tal.redstonechips.circuit.Circuit;

/**
 *
 * @author Tal Eisenberg
 */
public class CommandUtils {
    /**
     * Maximum number of lines that can fit on the minecraft screen.
     */
    public final static int MaxLines = 20;

    /**
     * Regards air and water as transparent materials when trying to find the block a player is looking at.
     */
    static final HashSet<Byte> transparentMaterials = new HashSet<Byte>();
    static {
        transparentMaterials.add((byte)Material.AIR.getId());
        transparentMaterials.add((byte)Material.WATER.getId());
        transparentMaterials.add((byte)Material.STATIONARY_WATER.getId());
    }


    public static Player checkIsPlayer(RedstoneChips rc, CommandSender sender) {
        if (sender instanceof Player) return (Player)sender;
        else {
            sender.sendMessage(rc.getPrefs().getErrorColor() + "Only players are allowed to use this command.");
            return null;
        }
    }

    public static Circuit findTargetCircuit(RedstoneChips rc, CommandSender sender) {
        Player player = checkIsPlayer(rc, sender);
        if (player==null) return null;

        Block target = targetBlock(player);
        Circuit c = rc.getCircuitManager().getCircuitByStructureBlock(target.getLocation());
        if (c==null) {
            sender.sendMessage(rc.getPrefs().getErrorColor() + "You need to point at a block of a redstone chip.");
        }

        return c;
    }

    public static Block targetBlock(Player player) {
        return player.getTargetBlock(transparentMaterials, 100);
    }

    public static boolean checkPermission(RedstoneChips rc, CommandSender sender, String commandName, boolean opRequired, boolean report) {
        if (!rc.getPrefs().getUsePermissions()) return (opRequired?sender.isOp():true);
        if (!(sender instanceof Player)) return true;
        if(((Player)sender).hasPermission("redstonechips.command." + commandName) && !((Player)sender).hasPermission("redstonechips.command." + commandName + ".deny")) {
            return true;
        } else {
            if (report) sender.sendMessage(rc.getPrefs().getErrorColor() + "You do not have permission to use command " + commandName + ".");
            return false;
        }
    }

    public static Map<CommandSender, PageInfo> playerPages = new HashMap<CommandSender, PageInfo>();

    public static void pageMaker(CommandSender s, String title, String commandName, String text, ChatColor infoColor, ChatColor errorColor) {
        CommandUtils.pageMaker(s, title, commandName, text, infoColor, errorColor, MaxLines);
    }

    public static void pageMaker(CommandSender s, String title, String commandName, String text, ChatColor infoColor, ChatColor errorColor, int maxLines) {
        String lines[];
        if (s instanceof Player)
            lines = wrapText(text);
        else lines = text.split("\n");
        
        CommandUtils.pageMaker(s, title, commandName, lines, infoColor, errorColor, maxLines);
    }
    
    public static void pageMaker(CommandSender s, String title, String commandName, String[] lines, ChatColor infoColor, ChatColor errorColor) {
        CommandUtils.pageMaker(s, title, commandName, lines, infoColor, errorColor, MaxLines);
    }

    public static void pageMaker(CommandSender s, String title, String commandName, String[] lines, ChatColor infoColor, ChatColor errorColor, int maxLines) {
        maxLines = maxLines - 4;
        int page;

        int pageCount = (int)(Math.ceil(lines.length/(float)maxLines));
        if (commandName!=null || !playerPages.containsKey(s)) {
            page = 1;
            playerPages.put(s, new PageInfo(title, pageCount, lines, maxLines+4, infoColor, errorColor));
        } else {
            PageInfo pageInfo = playerPages.get(s);
            page = pageInfo.page;
        } 


        if (page<1 || page>pageCount) s.sendMessage(errorColor + "Invalid page number: " + page + ". Expecting 1-" + pageCount);
        else {
            s.sendMessage(infoColor + title + ": " + (pageCount>1?"( page " + page + " / " + pageCount  + " )":""));
            s.sendMessage(infoColor + "----------------------");
            for (int i=(page-1)*maxLines; i<Math.min(lines.length, page*maxLines); i++) {
                s.sendMessage(lines[i]);
            }
            s.sendMessage(infoColor + "----------------------");
            if (pageCount>1) s.sendMessage("Use " + ChatColor.YELLOW + (s instanceof Player?"/":"") + "rcp [page#|next|prev|last]" + ChatColor.WHITE + " to see other pages.");
        }
    }

    private static final int CHAT_WINDOW_WIDTH = 320;
    private static final int CHAT_STRING_LENGTH = 119;
    private static final char COLOR_CHAR = '\u00A7';
    
    // fixed char width until a better solution comes by.
    private static final int CHAR_WIDTH = 6;
    private static final int SPACE_WIDTH = 4;
    private static final String TWO_PIXEL_CHARS = "!,.|:'i;";
    private static final String THREE_PIXEL_CHARS = "l'";
    private static final String FOUR_PIXEL_CHARS = "It[]";
    private static final String FIVE_PIXEL_CHARS = "f<>(){}";
    
    private static String[] wrapText(String text) {
        final StringBuilder out = new StringBuilder();

        int lineWidth = 0;
        int lineLength = 0;
        char colorChar = 'f';
        
        // Go over the message char by char.
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch=='\n') {
                lineLength = 0;
                lineWidth = 0;
            }
            
            // Get the color
            if (ch == COLOR_CHAR && i < text.length() - 1) {
                // We might need a linebreak ... so ugly ;(
                if (lineLength + 2 > CHAT_STRING_LENGTH) {
                    out.append('\n');
                    lineLength = 0;
                    if (colorChar != 'f' && colorChar != 'F') {
                        out.append(COLOR_CHAR).append(colorChar);
                        lineLength += 2;
                    }
                }
                colorChar = text.charAt(++i);
                out.append(COLOR_CHAR).append(colorChar);
                lineLength += 2;
                continue;
            }
            
            // See if we need a linebreak
            if (lineLength + 1 > CHAT_STRING_LENGTH || lineWidth + CHAR_WIDTH >= CHAT_WINDOW_WIDTH) {
                out.append('\n');
                lineLength = 0;
                lineWidth = getCharWidth(ch);

                // Re-apply the last color if it isn't the default
                if (colorChar != 'f' && colorChar != 'F') {
                    out.append(COLOR_CHAR).append(colorChar);
                    lineLength += 2;
                }                
            } else {
                lineWidth += getCharWidth(ch);
            }
            out.append(ch);
            lineLength++;
        }

        // Return it split
        return out.toString().split("\n");
    }
    
    private static int getCharWidth(char ch) {
        if (ch==' ') return SPACE_WIDTH;
        else if (TWO_PIXEL_CHARS.indexOf(ch)!=-1) return 2;
        else if (THREE_PIXEL_CHARS.indexOf(ch)!=-1) return 3;
        else if (FOUR_PIXEL_CHARS.indexOf(ch)!=-1) return 4;
        else if (FIVE_PIXEL_CHARS.indexOf(ch)!=-1) return 5;
        else return CHAR_WIDTH;
        
    }
}
package com.example.gestorgastos.data;

import java.util.ArrayList;
import java.util.List;

public class EmojiCategories {
    
    public static class EmojiCategory {
        public String name;
        public String icon;
        public String[] emojis;
        
        public EmojiCategory(String name, String icon, String[] emojis) {
            this.name = name;
            this.icon = icon;
            this.emojis = emojis;
        }
    }
    
    public static List<EmojiCategory> getAllCategories() {
        List<EmojiCategory> categories = new ArrayList<>();
        
        // Comida y Bebida
        categories.add(new EmojiCategory(
            "Comida y Bebida",
            "🍕",
            new String[]{
                "🍕", "🍔", "🍟", "🌭", "🥪", "🌮", "🌯", "🥙",
                "🥗", "🍜", "🍝", "🍲", "🍛", "🍣", "🍱", "🍙",
                "🍚", "🍘", "🍥", "🥟", "🍤", "🍢", "🍡", "🍧",
                "🍨", "🍦", "🥧", "🧁", "🍰", "🎂", "🍮", "🍭",
                "🍬", "🍫", "🍩", "🍪", "🌰", "🥜", "🍯", "🥛",
                "☕", "🍵", "🥤", "🧃", "🧉", "🍺", "🍻", "🥂",
                "🍷", "🍸", "🍹", "🧊", "🥄", "🍴", "🍽️", "🥢"
            }
        ));
        
        // Transporte
        categories.add(new EmojiCategory(
            "Transporte",
            "🚗",
            new String[]{
                "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑",
                "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🏍️", "🛵",
                "🚲", "🛴", "🛹", "🛼", "🚁", "✈️", "🛩️", "🛫",
                "🛬", "🪂", "💺", "🚀", "🛸", "🚉", "🚊", "🚝",
                "🚞", "🚋", "🚃"
            }
        ));
        
        // Entretenimiento
        categories.add(new EmojiCategory(
            "Entretenimiento",
            "🎬",
            new String[]{
                "🎬", "🎭", "🎨", "🎪", "🎡", "🎢", "🎠", "🎯",
                "🎲", "🎮", "🕹️", "🎰", "🎳", "🎴", "🃏",
                "🎵", "🎶", "🎤", "🎧", "🎸", "🎹", "🥁", "🎺",
                "🎷", "🎻", "🤹", "🤹‍♂️", "🤹‍♀️", "🖼️"
            }
        ));
        
        // Salud y Medicina
        categories.add(new EmojiCategory(
            "Salud y Medicina",
            "💊",
            new String[]{
                "💊", "💉", "🩺", "🩹", "🩼", "🦽", "🦯", "🩻",
                "🏥"
            }
        ));
        
        // Educación
        categories.add(new EmojiCategory(
            "Educación",
            "📚",
            new String[]{
                "📚", "📖", "📗", "📘", "📙", "📕", "📓", "📔",
                "📒", "📃", "📄", "📜", "📰", "🗞️", "📑", "🔖",
                "🏷️", "✏️", "✒️", "🖋️", "🖊️", "📝", "📏", "📐",
                "📌", "📍", "📎", "🖇️", "📋"
            }
        ));
        
        // Hogar y Familia
        categories.add(new EmojiCategory(
            "Hogar y Familia",
            "🏠",
            new String[]{
                "🏠", "🏡", "🏘️", "🏚️", "🏗️", "🏢", "🏬",
                "🏣", "🏤", "🏨", "🏩", "🏪", "🏫",
                "🏯", "🏰", "💒", "🏛️", "⛪", "🕌", "🛕", "🕍",
                "🕋", "⛩️", "🛤️", "🛣️", "🗾", "🎑", "🏞️", "🌅",
                "🌄", "🌠", "🎇", "🎆", "🌇", "🌆", "🏙️", "🌃",
                "🌌", "🌉", "🌁", "⛅", "⛈️", "🌤️", "🌦️", "🌧️"
            }
        ));
        
        // Tecnología
        categories.add(new EmojiCategory(
            "Tecnología",
            "📱",
            new String[]{
                "📱", "📲", "☎️", "📞", "📟", "📠", "🔋", "🔌",
                "💻", "🖥️", "🖨️", "⌨️", "🖱️", "🖲️", "💽", "💾",
                "💿", "📀", "🎥", "📷", "📸", "📹", "📼",
                "🔍", "🔎", "🔬", "🔭", "📡", "🕯️", "💡", "🔦",
                "🏮", "🪔", "🧪", "🧫", "🧬", "🔧", "🔨", "⚒️",
                "🛠️", "⛏️", "🔩", "⚙️", "🧰", "🧲", "⛓️"
            }
        ));
        
        // Deportes y Fitness
        categories.add(new EmojiCategory(
            "Deportes y Fitness",
            "⚽",
            new String[]{
                "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉",
                "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏",
                "🪃", "🥅", "⛳", "🪁", "🏹", "🎣", "🤿", "🥊",
                "🥋", "🎽", "🛷", "⛸️", "🥌", "🎿", "⛷️",
                "🏂", "🏋️", "🤼", "🤸", "⛹️", "🤺", "🤾",
                "🏌️", "🏇", "🧘", "🏄", "🏊", "🤽", "🚣", "🧗"
            }
        ));
        
        // Naturaleza
        categories.add(new EmojiCategory(
            "Naturaleza",
            "🌱",
            new String[]{
                "🌱", "🌿", "☘️", "🍀", "🎍", "🎋", "🍃", "🍂",
                "🍁", "🍄", "🐚", "🌾", "💐", "🌷", "🌹", "🥀",
                "🌺", "🌸", "🌼", "🌻", "🌞", "🌝", "🌛", "🌜",
                "🌚", "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓",
                "🌔", "🌙", "🌎", "🌍", "🌏", "🪐", "💫",
                "🌟", "💥", "🔥", "💢", "💯", "💨", "💦", "💤"
            }
        ));
        
        // Animales
        categories.add(new EmojiCategory(
            "Animales",
            "🐶",
            new String[]{
                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
                "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🙈",
                "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🐣",
                "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴",
                "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜", "🦟",
                "🦗", "🕷️", "🕸️", "🦂", "🐢", "🐍", "🦎", "🦖"
            }
        ));
        
        // Emociones y Caras
        categories.add(new EmojiCategory(
            "Emociones y Caras",
            "😀",
            new String[]{
                "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
                "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
                "😘", "😗", "😚", "😙", "😋", "😛", "😜", "🤪",
                "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨",
                "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥",
                "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢"
            }
        ));
        
        // Dinero y Finanzas
        categories.add(new EmojiCategory(
            "Dinero y Finanzas",
            "💰",
            new String[]{
                "💰", "💴", "💵", "💶", "💷", "💸", "💳", "💎",
                "🏦", "💼", "📊", "📈", "📉", "💹", "🔢", "🔣"
            }
        ));
        
        // Objetos y Símbolos
        categories.add(new EmojiCategory(
            "Objetos y Símbolos",
            "⭐",
            new String[]{
                "⭐", "🌟", "💫", "✨", "💥", "💢", "💯", "💨",
                "💦", "💤", "🕳️", "💣", "💬", "👁️‍🗨️", "🗨️", "🗯️",
                "💭", "👋", "🤚", "🖐️", "✋", "🖖", "👌",
                "🤏", "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉",
                "👆", "🖕", "👇", "☝️", "👍", "👎", "👊", "✊",
                "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏"
            }
        ));
        
        return categories;
    }
    
    public static String[] getAllEmojis() {
        List<String> allEmojis = new ArrayList<>();
        for (EmojiCategory category : getAllCategories()) {
            for (String emoji : category.emojis) {
                if (!allEmojis.contains(emoji)) {
                    allEmojis.add(emoji);
                }
            }
        }
        return allEmojis.toArray(new String[0]);
    }
    
    /**
     * Encuentra la categoría que contiene un emoji específico
     * @param emoji El emoji a buscar
     * @return La categoría que contiene el emoji, o null si no se encuentra
     */
    public static EmojiCategory findCategoryForEmoji(String emoji) {
        android.util.Log.d("EmojiCategories", "DEBUG - findCategoryForEmoji llamado con: '" + emoji + "'");
        if (emoji == null || emoji.trim().isEmpty()) {
            android.util.Log.d("EmojiCategories", "DEBUG - Emoji es null o vacío");
            return null;
        }
        
        for (EmojiCategory category : getAllCategories()) {
            android.util.Log.d("EmojiCategories", "DEBUG - Buscando en categoría: " + category.name);
            for (String categoryEmoji : category.emojis) {
                if (categoryEmoji.equals(emoji)) {
                    android.util.Log.d("EmojiCategories", "DEBUG - ¡Emoji encontrado! '" + emoji + "' en categoría: " + category.name);
                    return category;
                }
            }
        }
        android.util.Log.d("EmojiCategories", "DEBUG - Emoji '" + emoji + "' no encontrado en ninguna categoría");
        return null;
    }
    
    /**
     * Encuentra el índice de la categoría que contiene un emoji específico
     * @param emoji El emoji a buscar
     * @return El índice de la categoría, o 0 si no se encuentra
     */
    public static int findCategoryIndexForEmoji(String emoji) {
        android.util.Log.d("EmojiCategories", "DEBUG - Buscando índice para emoji: '" + emoji + "'");
        
        if (emoji == null || emoji.trim().isEmpty()) {
            android.util.Log.d("EmojiCategories", "DEBUG - Emoji es null o vacío");
            return 0;
        }
        
        List<EmojiCategory> categories = getAllCategories();
        for (int i = 0; i < categories.size(); i++) {
            EmojiCategory category = categories.get(i);
            android.util.Log.d("EmojiCategories", "DEBUG - Verificando categoría " + i + ": " + category.name);
            for (String categoryEmoji : category.emojis) {
                if (categoryEmoji.equals(emoji)) {
                    android.util.Log.d("EmojiCategories", "DEBUG - ¡Emoji '" + emoji + "' encontrado en categoría: " + category.name + " (índice: " + i + ")");
                    return i;
                }
            }
        }
        android.util.Log.d("EmojiCategories", "DEBUG - Emoji '" + emoji + "' no encontrado en ninguna categoría");
        return 0; // Fallback a primera categoría
    }
}

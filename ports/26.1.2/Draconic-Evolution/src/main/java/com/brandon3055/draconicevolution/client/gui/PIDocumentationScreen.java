package com.brandon3055.draconicevolution.client.gui;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native viewer for the unabridged, attributed PI-Docs archive, not replacement prose. */
public final class PIDocumentationScreen extends Screen {
    public static final Identifier DOCUMENT=Identifier.parse("draconicevolution:guide/author-guide.json");
    private final Screen parent;
    private final JsonObject document;
    private final List<JsonObject> sections=new ArrayList<>();
    private final List<Integer> matches=new ArrayList<>();
    private final List<Button> sectionButtons=new ArrayList<>(), bodyButtons=new ArrayList<>();
    private final List<List<Element>> sheets=new ArrayList<>();
    private String query="",locale;
    private int section, sheet, listOffset, left, top, panelWidth, panelHeight, sidebar, bodyX, bodyWidth, bodyTop;
    private Button previousList,nextList,previousSheet,nextSheet;
    private record Element(String type,FormattedCharSequence text,JsonObject data,int y,int height) {}

    public PIDocumentationScreen(Screen parent) {
        super(Component.literal("Draconic Evolution · PI-Docs"));
        this.parent=parent;
        try(var stream=Minecraft.getInstance().getResourceManager().open(DOCUMENT);
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            document=JsonParser.parseReader(reader).getAsJsonObject();
            for(var entry:document.getAsJsonArray("pages"))sections.add(entry.getAsJsonObject());
            if(sections.isEmpty())throw new IllegalStateException("Author documentation is empty");
        } catch(Exception failure) {throw new IllegalStateException("Unable to load bundled author documentation",failure);}
    }

    private JsonObject localized(JsonObject page) {
        var locales=page.getAsJsonObject("locales");
        return locales.getAsJsonObject(locales.has(locale)?locale:"en_us");
    }
    private String sectionTitle(int index) {return localized(sections.get(index)).get("title").getAsString();}
    public int sectionCount(){return sections.size();}
    public int sheetCount(){return sheets.size();}
    public void selectSection(int index){
        section=Math.clamp(index,0,sections.size()-1);sheet=0;layoutDocument();updateBodyButtons();
    }
    public void selectSheet(int index){sheet=Math.clamp(index,0,sheets.size()-1);updateBodyButtons();}

    @Override protected void init() {
        locale=minecraft.options.languageCode.startsWith("ru")?"ru_ru":"en_us";
        panelWidth=Math.min(width-12,680);panelHeight=height-12;
        left=(width-panelWidth)/2;top=6;sidebar=Math.min(170,Math.max(100,panelWidth/3));
        bodyX=left+sidebar+14;bodyWidth=panelWidth-sidebar-28;bodyTop=top+51;
        sectionButtons.clear();bodyButtons.clear();
        var search=addRenderableWidget(new EditBox(font,left+8,top+29,sidebar-10,18,Component.translatable("gui.recipebook.search_hint")));
        search.setValue(query);
        search.setResponder(value->{query=value;listOffset=0;updateSectionButtons();});
        int rows=Math.max(1,(panelHeight-105)/22);
        for(int row=0;row<rows;row++){
            final int offset=row;
            sectionButtons.add(addRenderableWidget(Button.builder(Component.empty(),b->{int match=listOffset+offset;if(match<matches.size())selectSection(matches.get(match));})
                    .bounds(left+8,top+53+row*22,sidebar-10,20).build()));
        }
        previousList=addRenderableWidget(Button.builder(Component.literal("<"),b->{listOffset=Math.max(0,listOffset-sectionButtons.size());updateSectionButtons();}).bounds(left+8,top+panelHeight-45,32,18).build());
        nextList=addRenderableWidget(Button.builder(Component.literal(">"),b->{listOffset+=sectionButtons.size();updateSectionButtons();}).bounds(left+sidebar-34,top+panelHeight-45,32,18).build());
        previousSheet=addRenderableWidget(Button.builder(Component.literal("<"),b->selectSheet(sheet-1)).bounds(bodyX,top+panelHeight-45,32,18).build());
        nextSheet=addRenderableWidget(Button.builder(Component.literal(">"),b->selectSheet(sheet+1)).bounds(left+panelWidth-42,top+panelHeight-45,32,18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(bodyX+bodyWidth/2-35,top+panelHeight-45,70,18).build());
        updateSectionButtons();layoutDocument();updateBodyButtons();
    }

    private void updateSectionButtons() {
        matches.clear();
        for(int i=0;i<sections.size();i++){
            var page=sections.get(i);
            if(query.isBlank()&&page.has("hidden")&&page.get("hidden").getAsBoolean())continue;
            if(sectionTitle(i).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))matches.add(i);
        }
        listOffset=Math.min(listOffset,Math.max(0,matches.size()-1));
        for(int i=0;i<sectionButtons.size();i++){
            var button=sectionButtons.get(i);int index=listOffset+i;
            button.visible=index<matches.size();
            if(button.visible){String name=sectionTitle(matches.get(index));button.setMessage(Component.literal(fitLabel(name,sidebar-20)));button.setTooltip(Tooltip.create(Component.literal(name)));}
        }
        if(previousList!=null){previousList.active=listOffset>0;nextList.active=listOffset+sectionButtons.size()<matches.size();}
    }

    private String fitLabel(String text,int maxWidth){
        return font.width(text)<=maxWidth?text:font.plainSubstrByWidth(text,Math.max(1,maxWidth-font.width("…")))+"…";
    }

    private ItemStack previewStack(JsonObject data,Identifier id){
        var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        if(data.has("soul_entity")&&stack.getItem() instanceof com.brandon3055.draconicevolution.items.MobSoul soul){
            var entity=Identifier.tryParse(data.get("soul_entity").getAsString());
            if(entity!=null)soul.setEntity(entity,stack);
        }
        return stack;
    }

    private void layoutDocument() {
        sheets.clear();sheets.add(new ArrayList<>());
        int y=0,available=Math.max(24,panelHeight-110);
        for(var entry:localized(sections.get(section)).getAsJsonArray("blocks")){
            var block=entry.getAsJsonObject();String type=block.get("type").getAsString();
            if(type.equals("text")){
                for(String paragraph:block.get("text").getAsString().split("\\R",-1)){
                    for(var line:font.split(Component.literal(paragraph.isBlank()?" ":paragraph),bodyWidth)){
                        if(y+font.lineHeight+2>available){sheets.add(new ArrayList<>());y=0;}
                        sheets.getLast().add(new Element("text",line,block,y,font.lineHeight+2));y+=font.lineHeight+2;
                    }
                }
                y+=4;
            }else{
                int height=20;
                if(type.equals("image")){
                    var image=document.getAsJsonObject("images").getAsJsonObject(block.get("key").getAsString());
                    if(image==null||!image.has("texture"))continue;
                    block=image;
                    height=Math.min(available,Math.min(150,Math.max(24,bodyWidth*image.get("height").getAsInt()/Math.max(1,image.get("width").getAsInt()))));
                }
                if(y+height>available){sheets.add(new ArrayList<>());y=0;}
                sheets.getLast().add(new Element(type,null,block,y,height));y+=height+5;
            }
        }
        sheet=Math.clamp(sheet,0,sheets.size()-1);
    }

    private void follow(String target) {
        for(int i=0;i<sections.size();i++)if(sections.get(i).get("id").getAsString().equals(target)){selectSection(i);return;}
        if(target.startsWith("https://")||target.startsWith("http://"))
            net.minecraft.client.gui.screens.ConfirmLinkScreen.confirmLinkNow(this,target);
    }
    private void updateBodyButtons() {
        for(var button:bodyButtons)removeWidget(button);bodyButtons.clear();
        if(previousSheet==null)return;
        previousSheet.active=sheet>0;nextSheet.active=sheet+1<sheets.size();
        for(var element:sheets.get(sheet))if(element.type.equals("link")){
            var data=element.data;String label=data.get("text").getAsString();
            var button=addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(label,bodyWidth-10)),b->follow(data.get("target").getAsString()))
                    .bounds(bodyX,bodyTop+element.y,bodyWidth,element.height).tooltip(Tooltip.create(Component.literal(label))).build());
            bodyButtons.add(button);
        }
        for(var element:sheets.get(sheet))if(element.type.equals("recipe")||element.type.equals("stack")){
            var id=Identifier.tryParse(element.data.get("item").getAsString());
            if(id==null||!BuiltInRegistries.ITEM.containsKey(id))continue;
            var button=addRenderableWidget(Button.builder(Component.literal(locale.equals("ru_ru")?"Рецепт":"Recipe"),b->displayRecipesFor(new ItemStack(BuiltInRegistries.ITEM.getValue(id))))
                    .bounds(bodyX+bodyWidth-48,bodyTop+element.y,48,element.height).build());
            button.active=net.neoforged.fml.ModList.get().isLoaded("emi");bodyButtons.add(button);
        }
    }

    public static void displayRecipesFor(ItemStack stack) {
        try {
            Class<?> ingredient=Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            Object displayed=Class.forName("dev.emi.emi.api.stack.EmiStack").getMethod("of",ItemStack.class).invoke(null,stack);
            Class.forName("dev.emi.emi.api.EmiApi").getMethod("displayRecipes",ingredient).invoke(null,displayed);
        } catch(ReflectiveOperationException failure) {throw new IllegalStateException("Unable to open EMI recipe from documentation",failure);}
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        graphics.fill(left,top,left+panelWidth,top+panelHeight,0xF01D252C);
        graphics.fill(left+sidebar+5,top+25,left+sidebar+6,top+panelHeight-22,0xFF6C6453);
        graphics.centeredText(font,title,width/2,top+8,0xFFFFD082);
        var heading=font.split(Component.literal(sectionTitle(section)),bodyWidth);
        for(int line=0;line<Math.min(2,heading.size());line++)graphics.text(font,heading.get(line),bodyX,top+27+line*font.lineHeight,0xFFFFD082,false);
        for(var element:sheets.get(sheet)){
            int y=bodyTop+element.y;
            switch(element.type){
                case "text"->graphics.text(font,element.text,bodyX,y,0xFFF0ECE2,false);
                case "image"->{
                    var data=element.data;int originalWidth=data.get("width").getAsInt(),originalHeight=data.get("height").getAsInt();
                    int drawWidth=Math.min(bodyWidth,element.height*originalWidth/Math.max(1,originalHeight));
                    graphics.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse(data.get("texture").getAsString()),bodyX+(bodyWidth-drawWidth)/2,y,0,0,drawWidth,element.height,originalWidth,originalHeight,originalWidth,originalHeight);
                }
                case "stack","recipe"->{
                    String name=element.data.get("item").getAsString();Identifier id=Identifier.tryParse(name);
                    if(id!=null&&BuiltInRegistries.ITEM.containsKey(id)){
                    var item=previewStack(element.data,id);graphics.item(item,bodyX,y);
                        graphics.text(font,font.plainSubstrByWidth(item.getHoverName().getString(),Math.max(1,bodyWidth-74)),bodyX+22,y+4,0xFFF0ECE2,false);
                        if(mouseX>=bodyX&&mouseX<bodyX+16&&mouseY>=y&&mouseY<y+16)graphics.setTooltipForNextFrame(font,item,mouseX,mouseY);
                    }else {
                        String archived=element.data.has("display_name")?element.data.get("display_name").getAsString():(locale.equals("ru_ru")?"Предмет из архивной версии":"Archived item");
                        graphics.text(font,fitLabel(archived,bodyWidth),bodyX,y+4,0xFFB7B7B7,false);
                    }
                }
            }
        }
        graphics.text(font,(sheet+1)+" / "+sheets.size(),bodyX+38,top+panelHeight-40,0xFFCCCCCC,false);
        String credit=locale.equals("ru_ru")?"Авторские тексты и иллюстрации · PI-Docs 3.0.0 (архив)":"Original text and illustrations · PI-Docs 3.0.0 (archive)";
        graphics.centeredText(font,font.plainSubstrByWidth(credit,panelWidth-12),width/2,top+panelHeight-15,0xFFA9A28F);
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}

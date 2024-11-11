package level.procedural.marker.resolved;

import foundation.math.ObjPos;
import level.procedural.generator.GeneratorType;
import level.procedural.marker.LMType;
import loader.JsonObject;
import loader.JsonType;
import render.RenderOrder;
import render.Renderable;
import render.renderables.RenderGameSquare;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;

//A more specific element resolved from LMElementType that defines exactly what is to be generated
//at this marker, based on region
public class LMTResolvedElement extends LMType {
    private static final HashMap<String, LMTResolvedElement> elements = new HashMap<>();

    public static final LMTResolvedElement ISLAND_FLOATING = new LMTResolvedElement(
            "island_floating",
            new RenderGameSquare(RenderOrder.DEBUG, Color.BLUE, 0.5f, () -> new ObjPos(0.5f, 0.5f)));
    public static final LMTResolvedElement ISLAND_SIDE_ATTACHED = new LMTResolvedElement(
            "island_attached",
            new RenderGameSquare(RenderOrder.DEBUG, Color.BLUE, 0.5f, () -> new ObjPos(0.5f, 0.5f)));

    private final ArrayList<GenerationElement> generatorConditions = new ArrayList<>();

    protected LMTResolvedElement(String s, Renderable debugRenderable) {
        super(s, debugRenderable);
        elements.put(s, this);
    }

    public static LMTResolvedElement getElementType(String name) {
        LMTResolvedElement resolvedElement = elements.get(name);
        if (resolvedElement == null)
            throw new IllegalArgumentException("Unrecognised LMTResolvedElement name \"" + name + "\"");
        return resolvedElement;
    }

    public GeneratorType getGenerator(GeneratorConditionData data) {
        for (GenerationElement generationElement : generatorConditions) {
            if (generationElement.condition.test(data)) {
                return GeneratorType.getGeneratorType(generationElement.name());
            }
        }
        throw new IllegalArgumentException("LMTResolvedElement \"" + s + "\" was not able to be generated as none of the conditions returned true");
    }

    @Override
    public void parseDataFromJson(JsonObject data) {
        data.get("generatesAs", JsonType.JSON_ARRAY_TYPE).forEach(element -> {
                    generatorConditions.add(new GenerationElement(
                            o -> ((boolean) GeneratorCondition.parser.parseExpression(element.getOrDefault("condition", "true", JsonType.STRING_JSON_TYPE)).apply(o)),
                            element.get("name", JsonType.STRING_JSON_TYPE)
                    ));
                }, JsonType.JSON_OBJECT_TYPE
        );
    }

    private record GenerationElement(Predicate<GeneratorConditionData> condition, String name) {
    }
}

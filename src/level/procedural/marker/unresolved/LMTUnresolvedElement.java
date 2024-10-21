package level.procedural.marker.unresolved;

import foundation.ObjPos;
import level.procedural.marker.resolved.LMTResolvedElement;
import level.procedural.marker.LMType;
import loader.JsonObject;
import loader.JsonType;
import render.RenderOrder;
import render.Renderable;
import render.renderables.RenderGameSquare;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Predicate;

//General element type markers that have not been resolved to something more specific based on
//the surrounding blocks
public class LMTUnresolvedElement extends LMType {
    public static final LMTUnresolvedElement PLATFORM = new LMTUnresolvedElement(
            "platform",
            new RenderGameSquare(RenderOrder.BLOCK, Color.RED, 0.5f, () -> new ObjPos(0.5f, 0.5f))
    );

    private final ArrayList<ResolveElement> resolveConditions = new ArrayList<>();

    protected LMTUnresolvedElement(String s, Renderable debugRenderable) {
        super(s, debugRenderable);
    }

    public LMTResolvedElement resolve(ResolverConditionData data) {
        for (ResolveElement resolveElement : resolveConditions) {
            if (resolveElement.condition.test(data)) {
                return LMTResolvedElement.getElementType(resolveElement.name());
            }
        }
        throw new IllegalArgumentException("LMTUnresolvedElement \"" + s + "\" was not able to be resolved as none of the conditions returned true");
    }

    @Override
    public void parseDataFromJson(JsonObject data) {
        data.get("resolvesTo", JsonType.JSON_ARRAY_TYPE).forEach(element -> {
                    resolveConditions.add(new ResolveElement(
                            o -> ((boolean) ResolverCondition.parser.parseExpression(element.getOrDefault("condition", "true", JsonType.STRING_JSON_TYPE)).apply(o)),
                            element.get("name", JsonType.STRING_JSON_TYPE)
                    ));
                }, JsonType.JSON_OBJECT_TYPE
        );
    }

    private record ResolveElement(Predicate<ResolverConditionData> condition, String name) {
    }
}

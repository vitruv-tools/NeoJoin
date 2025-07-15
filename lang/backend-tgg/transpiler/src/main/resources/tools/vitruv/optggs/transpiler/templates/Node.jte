@import tools.vitruv.optggs.transpiler.NameResolver
@import tools.vitruv.optggs.transpiler.tgg.Attribute
@import tools.vitruv.optggs.transpiler.tgg.Link
@import tools.vitruv.optggs.transpiler.tgg.Node
@import tools.vitruv.optggs.transpiler.tgg.Property
@param Node node
@param NameResolver nameResolver
@if(node.isGreen())++@endif ${node.id()}: ${nameResolver.resolveName(node.type())} {
    @for(Property prop : node.properties())${prop.toExpression(node)}
    @endfor
    @for(Attribute attr : node.attributes())@template.Attribute(node = node, attribute = attr)
    @endfor
    @for(Link link : node.links())@template.Link(link = link)
    @endfor
}

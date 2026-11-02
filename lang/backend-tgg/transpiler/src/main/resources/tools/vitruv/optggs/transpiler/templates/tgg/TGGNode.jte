@import tools.vitruv.optggs.transpiler.NameResolver
@import tools.vitruv.optggs.transpiler.graph.Attribute
@import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode
@import tools.vitruv.optggs.transpiler.graph.tgg.TGGLink
@param TGGNode node
@param NameResolver nameResolver
@if(node.isGreen())++@endif ${node.getId()}: ${nameResolver.resolveName(node.getType())} {
    @for(Attribute attr : node.attributes())@template.tgg.TGGAttribute(node = node, attribute = attr)
    @endfor

    @for(TGGLink link : node.links())@template.tgg.TGGLink(link = link)
    @endfor
}

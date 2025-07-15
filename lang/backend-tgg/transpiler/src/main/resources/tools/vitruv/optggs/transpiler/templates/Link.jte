@import tools.vitruv.optggs.transpiler.tgg.Link
@param Link link
@if(link.isGreen())++@endif-${link.name()}->${link.target().id()}

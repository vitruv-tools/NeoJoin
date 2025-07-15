import neo4j from 'neo4j-driver';
import config from 'config';
import Handlebars from 'handlebars';
import fs from 'fs';

let ignoreAttributes = ['ename', 'enamespace'];
let internalAttributes = ['_cr_','_de_'];
let nonStringAttributes = ['_cr_','_de_'];

const driver = neo4j.driver(
    config.get('connectionUri'),
    neo4j.auth.basic(config.get('username'), config.get('password')),
    { disableLosslessIntegers: true }
);

let allNames = [];
let idToName = {};
function getObjectName(id, className) {
    if(idToName[id]) return idToName[id];
    let name = className.replace(/[^A-Z]/g, '').toLocaleLowerCase()
    if (name == '') { name = className.charAt(0).toLocaleLowerCase() }
    let uniqueName = name;
    let count = 2;
    while (allNames.includes(uniqueName)) {
        uniqueName = name + count++
    }
    idToName[id] = uniqueName;
    allNames.push(uniqueName);
    return uniqueName;
}

async function getAttributeTypes(types) {
    let attributeTypes = {};
    const session = driver.session();
    let results = await session.run(`
        MATCH (n:NeoCore__EClass)-[:eAttributes]->(a)-[:eAttributeType]->(t)
        WHERE n.ename IN $types
        RETURN a.ename AS name, t.ename AS type
    `, { types });
    for (let i = 0; i < results.records.length; i++) {
        let record = results.records[i];
        let name = record.get(0);
        let attributeType = record.get(1);        
        attributeTypes[name] = attributeType;
    }
    session.close();
    return attributeTypes;
}

function cleanType(type) {
    return type.substr(type.indexOf('__')+2);
}

async function mapPropertiesToAttributes(props, types) {
    let attributes = [];
    let attributeTypes = await getAttributeTypes(types);
    let names = Object.keys(props).sort().filter(n => !ignoreAttributes.includes(n))
    for(let i = 0; i < names.length; i++){
        let name = names[i];
        let value = props[name];
        let isInternal = internalAttributes.includes(name) || !attributeTypes[name];
        if((attributeTypes[name] && attributeTypes[name] == 'EString') || isInternal && !nonStringAttributes.includes(name))
            value = new Handlebars.SafeString(`"${value}"`);                
        attributes.push({name, value, isInternal})
    }
    return attributes;
}

async function getNodes(modelName) {
    let nodes = [];
    const session = driver.session();
    let results = await session.run(`
        MATCH (n) 
        WHERE n.enamespace = $modelName
        WITH id(n) as id, labels(n)[1] as type, labels(n) as types, properties(n) as props
        ORDER BY type, id
        RETURN id, type, types, props
    `, { modelName });
    for (let i = 0; i < results.records.length; i++) {
        let record = results.records[i];
        let id = record.get(0);
        let type = cleanType(record.get(1));
        let types = record.get(2).map(cleanType);
        type = types.slice(1).join(",")
        let attributes = await mapPropertiesToAttributes(record.get(3), types);
        let associations = await getRelationships(id);
        nodes.push({ name: getObjectName(id, type), type, attributes, associations });
    }
    session.close();
    return nodes;
}

async function getRelationships(id) {
    let relationships = [];
    const session = driver.session();
    let results = await session.run(`
        MATCH (s)-[r]->(t)
        WHERE id(s) = $id
        WITH id(s) as id, type(r) as type, properties(r) as props, labels(t)[size(labels(t))-1] as targetType, id(t) as targetId
        ORDER BY type, id
        RETURN type, props, targetType, targetId
        `, { id });
    for (let i = 0; i < results.records.length; i++) {
        let record = results.records[i];
        let name = record.get(0);
        let attributes = await mapPropertiesToAttributes(record.get(1), [name]);
        let targetType = cleanType(record.get(2));
        let targetId = record.get(3);
        relationships.push({ name, attributes, target: getObjectName(targetId, targetType) });
    }
    session.close();
    return relationships;
}

async function dumpModel(name) {
    let objects = await getNodes(name);
    return { name, objects };
}

async function extractModels(modelNames) {
    let models = await Promise.all(modelNames.map(dumpModel));
    let emslModelTemplate = fs.readFileSync('emsl-model.hjs').toString();
    let emslModelGenerator = Handlebars.compile(emslModelTemplate);
    let emslModels = models.map(emslModelGenerator);
    emslModels.forEach(m => console.log(m));
    driver.close();
}

extractModels(["Source", "Target"]);
